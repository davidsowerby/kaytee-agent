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
import uk.q3c.simplycd.agent.eventbus.GlobalBusModule
import uk.q3c.simplycd.agent.i18n.I18NModule
import uk.q3c.simplycd.agent.lifecycle.LifecycleModule
import uk.q3c.simplycd.agent.project.DefaultProject
import uk.q3c.simplycd.agent.queue.BuildCompletedMessage
import uk.q3c.simplycd.agent.queue.QueueModule
import uk.q3c.simplycd.agent.system.SystemModule
import uk.q3c.simplycd.build.BuildNumberReader
import uk.q3c.simplycd.lifecycle.prepare.PreparationStage
import uk.q3c.simplycd.project.Project
import uk.q3c.simplycd.queue.*
import uk.q3c.simplycd.system.InstallationInfo

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
    QueueMessageReceiver queueMessageReceiver


    RequestQueue queue
    Project projectA
    Project projectB
    Project projectC
    Project projectD
    List<Project> projects = new ArrayList<>()


    static class TestBuildModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(BuildNumberReader.class).to(TestBuildNumberReader.class)
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
                    .implement(GradleTaskRequest.class, DefaultGradleTaskRequest.class)
                    .build(GradleTaskRequestFactory.class))
        }
    }

    static class BuildRequestGenerator implements Runnable {


        int generationPeriod = 3
        RequestQueue queue
        List<Project> projects

        BuildRequestGenerator(int generationPeriod, RequestQueue queue, List<Project> projects) {
            this.projects = projects
            this.generationPeriod = generationPeriod
            this.queue = queue
        }

        @Override
        void run() {
            int i = 0
            LocalDateTime targetEnd = LocalDateTime.now().plusSeconds(generationPeriod)
            Random random = new Random()
            int projectIndex = random.nextInt(4)
            queue.addRequest(projects.get(projectIndex), sha(i))

            while (LocalDateTime.now().isBefore(targetEnd)) {
                int delay = random.nextInt(800) + 200
                projectIndex = random.nextInt(4)
                Thread.sleep(delay)
                i++
                queue.addRequest(projects.get(projectIndex), sha(i))
            }

        }

        private GitSHA sha(int i) {
            return new GitSHA(DigestUtils.sha1Hex(i.toString()))
        }
    }


    def setup() {
        temp = temporaryFolder.getRoot()
        projectA = new DefaultProject('davidsowerby', 'projectA')
        projectB = new DefaultProject('davidsowerby', 'projectB')
        projectC = new DefaultProject('davidsowerby', 'projectC')
        projectD = new DefaultProject('davidsowerby', 'projectD')

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

        Injector injector = Guice.createInjector(bindings)
        queue = injector.getInstance(RequestQueue)
        InstallationInfo installationInfo = injector.getInstance(InstallationInfo)
        installationInfo.dataDirRoot = temp
        queueMessageReceiver = injector.getInstance(QueueMessageReceiver)
    }

    def cleanup() {
    }

    def "Single runner"() {
        given:
        Thread requestGeneratorThread = new Thread(new BuildRequestGenerator(5 * 1, queue, projects))
        requestGeneratorThread.run()

        when:
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(10)
        while (!queueMessageReceiver.finishedBuilds() && LocalDateTime.now().isBefore(timeout)) {
            Thread.sleep(100)
        }


        then:
        println "Build requests received: " + queueMessageReceiver.buildRequests.size()
        println "Build requests started: " + queueMessageReceiver.buildStarts.size()
        println "Build requests completed: " + queueMessageReceiver.buildCompletions.size()
        println "Preparations started: " + queueMessageReceiver.preparationStarts.size()
        println "Preparations completed: " + queueMessageReceiver.preparationCompletions.size()
        println "Tasks requests received: " + queueMessageReceiver.taskRequests.size()
        println "Tasks requests started: " + queueMessageReceiver.taskStarts.size()
        println "Tasks requests completed: " + queueMessageReceiver.taskCompletions.size()
        println "Build results held by "
        queueMessageReceiver.buildRequests.size() == queueMessageReceiver.buildStarts.size()
        queueMessageReceiver.buildCompletions.size() == queueMessageReceiver.buildStarts.size()
        queueMessageReceiver.preparationStarts.size() == queueMessageReceiver.preparationCompletions.size()
        queueMessageReceiver.taskRequests.size() == queueMessageReceiver.taskStarts.size()
        queueMessageReceiver.taskStarts.size() == queueMessageReceiver.taskCompletions.size()
        println 'Build completion order:'
        for (BuildCompletedMessage msg : queueMessageReceiver.buildCompletions) {
            println "$msg.project.name $msg.buildNumber"
        }

    }


}
