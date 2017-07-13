package uk.q3c.kaytee.agent.build

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.util.Modules
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.build.gitplus.GitPlusModule
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.app.ApiModule
import uk.q3c.kaytee.agent.app.Hooks
import uk.q3c.kaytee.agent.eventbus.GlobalBusModule
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.I18NModule
import uk.q3c.kaytee.agent.lifecycle.LifecycleModule
import uk.q3c.kaytee.agent.prepare.PreparationStage
import uk.q3c.kaytee.agent.project.DefaultProject
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.project.ProjectModule
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.kaytee.agent.system.SystemModule

import java.time.Duration
import java.time.LocalDateTime

/**
 * Integrates RequestQueue, BuildRunner and GradleExecutor
 *
 * The BuildRequestGenerator populates the RequestQueue with build requests
 *
 * <b>GradleTasks:<br></b>
 * The {@link MockPreparationStage} is used via {@link Soak_ITest.TestLifecycleModule} to create a {@link MockGradleLauncher}, which in turn randomises Gradle task duration and success / failure
 *
 * <b>Delegated Tasks:<br></b>
 * There is no need to change anything further - a delegate build creates a custom Gradle task under the same conditions as above, so when the {@link MockGradleLauncher} proides a randomised result,
 * that should get back to the {@link DelegatedProjectTaskRunner} as normal
 *
 * Created by David Sowerby on 08 Jan 2017
 */
class Soak_ITest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp

    Hooks mockHooks = Mock(Hooks)

    BuildRecordCollator resultCollator

    RequestQueue queue
    Project projectA
    Project projectB
    Project projectC
    Project projectD
    List<Project> projects = new ArrayList<>()

    static int buildsRequested = 0


    GlobalBusMonitor busMonitor
    static ArrayList<UUID> originalBuildRequests

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
            bind(PreparationStage.class).to(MockPreparationStage.class)
        }
    }

    static class TestQueueModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(ManualTaskLauncher.class).to(MockManualTaskLauncher.class)
            install(new FactoryModuleBuilder()
                    .implement(GradleTaskRunner.class, DefaultGradleTaskRunner.class)
                    .build(GradleTaskRunnerFactory.class))
            bind(GlobalBusMonitor)
        }
    }

    static class BuildRequestGenerator implements Runnable {


        int generationPeriod = 3
        RequestQueue queue
        List<Project> projects
        LocalDateTime targetEnd

        BuildRequestGenerator(int generationPeriod, RequestQueue queue, List<Project> projects) {
            this.projects = projects
            this.generationPeriod = generationPeriod
            this.queue = queue
        }

        @Override
        void run() {
            LocalDateTime started = LocalDateTime.now()
            println "Started request generation at ${started}"
            int i = 0
            targetEnd = LocalDateTime.now().plusSeconds(generationPeriod)
            Random random = new Random()
            int projectIndex = random.nextInt(4)
            originalBuildRequests.add(queue.addRequest(projects.get(projectIndex), sha(i)))
            buildsRequested++


            while (LocalDateTime.now().isBefore(targetEnd)) {
                int delay = random.nextInt(300) + 200
                projectIndex = random.nextInt(4)
                Thread.sleep(delay)
                i++
                originalBuildRequests.add(queue.addRequest(projects.get(projectIndex), sha(i)))
                buildsRequested++
            }
            LocalDateTime finished = LocalDateTime.now()
            println "Finished request generation at ${finished}"
            println "${buildsRequested} requests generated in: ${Duration.between(started, finished).toMillis() / 1000} seconds"
        }

        private GitSHA sha(int i) {
            return new GitSHA(DigestUtils.sha1Hex(i.toString()))
        }
    }

    int buildsCompleted = 0

    def setup() {
        temp = temporaryFolder.getRoot()
        projectA = new DefaultProject('davidsowerby/projectA', UUID.randomUUID())
        projectB = new DefaultProject('davidsowerby/projectB', UUID.randomUUID())
        projectC = new DefaultProject('davidsowerby/projectC', UUID.randomUUID())
        projectD = new DefaultProject('davidsowerby/projectD', UUID.randomUUID())

        projects.add(projectA)
        projects.add(projectB)
        projects.add(projectC)
        projects.add(projectD)

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

        Injector injector = Guice.createInjector(bindings)
        queue = injector.getInstance(RequestQueue)
        InstallationInfo installationInfo = injector.getInstance(InstallationInfo)
        installationInfo.dataDirRoot = temp
        resultCollator = injector.getInstance(BuildRecordCollator)
        busMonitor = injector.getInstance(GlobalBusMonitor)
        originalBuildRequests = new ArrayList<UUID>()
    }

    def cleanup() {
    }

    def "Single runner"() {
        given:
        int requestGenerationPeriodInSeconds = 60
        Thread requestGeneratorThread = new Thread(new BuildRequestGenerator(requestGenerationPeriodInSeconds, queue, projects))
        requestGeneratorThread.run()
        Thread.sleep(200) // let queue fill up a bit so we don't finish before we start

        when:

        LocalDateTime timeout = LocalDateTime.now().plusSeconds(requestGenerationPeriodInSeconds + 25)

        //wait for queue to drain
        while (!allBuildsComplete() && LocalDateTime.now().isBefore(timeout)) {
            Thread.sleep(200)
        }
        resultCollator.updateBuildStateCount()

        then:
        println "Build requests generated: $buildsRequested"
        for (key in BuildStateKey) {
            println "${key} : ${resultCollator.buildStateCount.get(key)}"
        }

        println "Build results held by resultsCollator: " + resultCollator.records.size()
        List<UUID> delegatedBuilds = new ArrayList<>()
        for (UUID key in busMonitor.getMessages().keySet()) {
            if (!originalBuildRequests.contains(key)) {
                delegatedBuilds.add(key)
            }
        }
        println "\n\nDelegated build tasks: \n"
        for (UUID key in delegatedBuilds) {
            println key
        }
        buildsRequested == buildsCompleted
//        buildsSuccessFul + buildsFailed == buildsCompleted
        resultCollator.records.size() == buildsRequested


    }

    private boolean allBuildsComplete() {
        buildsCompleted = 0

        for (BuildRecord result : resultCollator.records.values()) {
            if (result.processingCompleted) {
                buildsCompleted++
            } else {
                println "${result.uid}  not completed"
            }
        }

        return buildsCompleted == buildsRequested

    }


}
