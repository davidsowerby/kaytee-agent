package uk.q3c.kaytee.agent.build

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.util.Modules
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll
import uk.q3c.build.gitplus.GitPlusModule
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.app.ApiModule
import uk.q3c.kaytee.agent.app.Hooks
import uk.q3c.kaytee.agent.eventbus.GlobalBusModule
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.I18NModule
import uk.q3c.kaytee.agent.lifecycle.LifecycleModule
import uk.q3c.kaytee.agent.prepare.PreparationStage
import uk.q3c.kaytee.agent.project.DefaultProject
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.project.ProjectModule
import uk.q3c.kaytee.agent.queue.DelegatedProjectTaskRunner
import uk.q3c.kaytee.agent.queue.GradleTaskRunnerFactory
import uk.q3c.kaytee.agent.queue.QueueModule
import uk.q3c.kaytee.agent.queue.RequestQueue
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.kaytee.agent.system.SystemModule
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.util.file.FileKUtilsModule

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Not_Applicable
import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Task_Failure
import static uk.q3c.kaytee.agent.i18n.BuildStateKey.*
import static uk.q3c.kaytee.plugin.TaskKey.Bintray_Upload
import static uk.q3c.kaytee.plugin.TaskKey.Custom
/**
 *
 * The {@link MockPreparationStage2} is used to avoid the need for disk access or code pull.  This stage can be made to fail
 * by setting failToRun to true
 *
 * A single request is made to the RequestQueue for each scenario.  Build configuration can be changed by modifying
 * or replacing {@link MockPreparationStage2#buildConfiguration}
 *
 * <b>GradleTasks:<br></b>
 * {@link MockGradleTaskRunner} s are created using {@link MockGradleTaskRunnerFactory}.  The latter is injected via the TestQueueModel
 * A task can be configured to fail by setting {@link MockGradleTaskRunnerFactory#failingTask}.  If this property is left as null
 * the build should be successful.
 *
 * The MockGradleTask randomises timings, but is always short in duration
 *
 * <b>Delegated Tasks:<br></b>
 * There is no need to change anything further - a delegate build creates a custom Gradle task under the same conditions as above, so when the {@link MockGradleLauncher} proides a randomised result,
 * that should get back to the {@link DelegatedProjectTaskRunner} as normal
 *
 * Created by David Sowerby on 08 Jan 2017
 */
class BuildTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp

    Hooks mockHooks = Mock(Hooks)

    MockGradleTaskRunnerFactory gradleTaskRunnerFactory
    GlobalBusProvider globalBusProvider

    RequestQueue queue
    Project projectA

    static ArrayList<UUID> originalBuildRequests
    BuildRecordCollator resultCollator
    MockPreparationStage2 preparationStage
    static KayTeeExtension defaultConfig = new KayTeeExtension()
    static KayTeeExtension configWithDelegatedTask

    static class TestBuildModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(BuildNumberReader.class).to(TestBuildNumberReader.class)
            bind(IssueCreator.class).toInstance(new MockIssueCreator())
        }
    }

    class TestApiModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(Hooks.class).toInstance(mockHooks)
        }
    }

    static class TestLifecycleModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(PreparationStage.class).to(MockPreparationStage2.class).asEagerSingleton()
        }
    }

    static class TestQueueModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(GradleTaskRunnerFactory.class).toInstance(new MockGradleTaskRunnerFactory())
        }
    }


    int buildsCompleted = 0

    def setupSpec() {
        configWithDelegatedTask = new KayTeeExtension()
        configWithDelegatedTask.functionalTest.enabled = true
        configWithDelegatedTask.functionalTest.delegated = true
        configWithDelegatedTask.functionalTest.delegate.repoName = 'wiggly'
        configWithDelegatedTask.functionalTest.delegate.repoUserName = 'davidsowerby'
        configWithDelegatedTask.functionalTest.delegate.commitId = sha(2)
    }

    def setup() {
        temp = temporaryFolder.getRoot()
        projectA = new DefaultProject('davidsowerby/projectA', UUID.randomUUID())
        List<Module> bindings = new ArrayList<>()
        bindings.add(new GlobalBusModule())
        bindings.add(Modules.override(new BuildModule()).with(new TestBuildModule()))
        bindings.add(new SystemModule())
        bindings.add(Modules.override(new QueueModule()).with(new TestQueueModule()))
        bindings.add(Modules.override(new LifecycleModule()).with(new TestLifecycleModule()))
        bindings.add(new GitPlusModule())
        bindings.add(new I18NModule())
        bindings.add(Modules.override(new ApiModule()).with(new TestApiModule()))
        bindings.add(new ProjectModule())
        bindings.add(new FileKUtilsModule())

        Injector injector = Guice.createInjector(bindings)
        queue = injector.getInstance(RequestQueue)
        InstallationInfo installationInfo = injector.getInstance(InstallationInfo)
        installationInfo.dataDirRoot = temp
        resultCollator = injector.getInstance(BuildRecordCollator)
        gradleTaskRunnerFactory = injector.getInstance(GradleTaskRunnerFactory)
        globalBusProvider = injector.getInstance(GlobalBusProvider)
        gradleTaskRunnerFactory.globalBusProvider = globalBusProvider
        originalBuildRequests = new ArrayList<UUID>()
        preparationStage = injector.getInstance(PreparationStage)
    }

    def cleanup() {
    }


    @Unroll
    def "All builds #desc"() {
        given:
        preparationStage.failOnRun = failPrep
        preparationStage.buildConfiguration = buildConfig
        gradleTaskRunnerFactory.failingTask = failingTask
        LocalDateTime timeout = LocalDateTime.now().plus(5, ChronoUnit.SECONDS)

        when:
        UUID requestId = queue.addRequest(projectA, sha(1))
        //wait for collator to catch up
        while (!resultCollator.hasRecord(requestId)) {
            println 'waiting to start'
            Thread.sleep(100)
        }
        while (!(resultCollator.getRecord(requestId).hasCompleted()) && LocalDateTime.now().isBefore(timeout)) {
            println 'waiting for build to complete'
            Thread.sleep(200)
        }
        BuildRecord record = resultCollator.getRecord(requestId)


        then:

        record.outcome == outcome
        record.state == BuildStateKey.Complete
        record.causeOfFailure == causeOfFailure

        where:

        desc                        | buildConfig             | failingTask    | failPrep | outcome            | causeOfFailure
        "Successful standard build" | defaultConfig           | null           | false    | Successful         | Not_Applicable
        "Successful with delegate"  | configWithDelegatedTask | null           | false    | Successful         | Not_Applicable
        "Prep fails"                | defaultConfig           | null           | true     | Preparation_Failed | BuildFailCauseKey.Preparation_Failed
        "Task fails"                | defaultConfig           | Bintray_Upload | false    | Failed             | Task_Failure
        "Delegate task fails"       | configWithDelegatedTask | Custom         | false    | Failed             | Task_Failure
    }


    private GitSHA sha(int i) {
        return new GitSHA(DigestUtils.sha1Hex(i.toString()))
    }

}
