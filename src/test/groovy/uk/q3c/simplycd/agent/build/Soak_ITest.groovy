package uk.q3c.simplycd.agent.build

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
import uk.q3c.simplycd.agent.app.ApiModule
import uk.q3c.simplycd.agent.app.Hooks
import uk.q3c.simplycd.agent.eventbus.GlobalBusModule
import uk.q3c.simplycd.agent.i18n.BuildStateKey
import uk.q3c.simplycd.agent.i18n.I18NModule
import uk.q3c.simplycd.agent.lifecycle.LifecycleModule
import uk.q3c.simplycd.agent.prepare.PreparationStage
import uk.q3c.simplycd.agent.project.DefaultProject
import uk.q3c.simplycd.agent.project.Project
import uk.q3c.simplycd.agent.project.ProjectModule
import uk.q3c.simplycd.agent.queue.*
import uk.q3c.simplycd.agent.system.InstallationInfo
import uk.q3c.simplycd.agent.system.SystemModule

import java.time.LocalDateTime
/**
 * Integrates RequestQueue, BuildRunner and GradleExecutor
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
    int preparationFailures = 0
    int buildsStarted = 0
    int buildsCompleted = 0
    def buildsSuccessFul = 0
    def buildsFailed = 0
    def preparationsFailed = 0


    static class TestBuildModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(BuildNumberReader.class).to(TestBuildNumberReader.class)
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
            bind(QueueMessageReceiver)
            install(new FactoryModuleBuilder()
                    .implement(GradleTaskRunner.class, DefaultGradleTaskRunner.class)
                    .build(GradleTaskRunnerFactory.class))
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
            int i = 0
            targetEnd = LocalDateTime.now().plusSeconds(generationPeriod)
            Random random = new Random()
            int projectIndex = random.nextInt(4)
            queue.addRequest(projects.get(projectIndex), sha(i))
            buildsRequested++

            while (LocalDateTime.now().isBefore(targetEnd)) {
                int delay = random.nextInt(800) + 200
                projectIndex = random.nextInt(4)
                Thread.sleep(delay)
                i++
                queue.addRequest(projects.get(projectIndex), sha(i))
                buildsRequested++
            }

        }

        private GitSHA sha(int i) {
            return new GitSHA(DigestUtils.sha1Hex(i.toString()))
        }
    }


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
    }

    def cleanup() {
    }

    def "Single runner"() {
        given:
        int requestGenerationPeriodInSeconds = 5
        Thread requestGeneratorThread = new Thread(new BuildRequestGenerator(requestGenerationPeriodInSeconds, queue, projects))
        requestGeneratorThread.run()

        when:

        LocalDateTime timeout = LocalDateTime.now().plusSeconds(requestGenerationPeriodInSeconds * 2)

        //wait for queue to drain
        while (!(queue.size() == 0)) {
            println ">>>>> Waiting for queue to empty"
            Thread.sleep(1000)
        }

        // wait for last jobs to complete
        println ">>>>> Waiting for last jobs to complete"
        Thread.sleep(2000)

        println ">>>>> Validating results"
        boolean allComplete = true
        List<String> validationErrors = new ArrayList<>()

        for (BuildRecord result : resultCollator.records.values()) {
            BuildRecordValidator resultValidator = new BuildRecordValidator(result)
            if (!result.requestedCompleted()) {
                allComplete = false
            } else {
                buildsCompleted++
            }
            switch (result.state) {
                case BuildStateKey.Successful: buildsSuccessFul++; break
                case BuildStateKey.Failed: buildsFailed++; break
                case BuildStateKey.Preparation_Failed: preparationsFailed++; break
            }
            resultValidator.validate()
            validationErrors.addAll(resultValidator.errors)
        }
        if (!validationErrors.isEmpty()) {
            println validationErrors
        }

        then:
        println "Build requests generated: $buildsRequested"
        println "Build requests failed: $buildsFailed"
        println "Build requests sucessful: $buildsSuccessFul"
        println "Build requests completed: $buildsCompleted"
        println "Preparations failed: $preparationsFailed"
        println "Build results held by resultsCollator: " + resultCollator.records.size()
        allComplete // this could fail if processing is not allowed to complete, and test finishes too early
        validationErrors.isEmpty()
        buildsRequested == buildsCompleted
        buildsSuccessFul + buildsFailed + preparationsFailed == buildsCompleted
        resultCollator.records.size() == buildsRequested


    }


}
