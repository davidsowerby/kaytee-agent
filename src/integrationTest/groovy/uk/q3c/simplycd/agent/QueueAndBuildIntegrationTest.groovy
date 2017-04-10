package uk.q3c.simplycd.agent

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.util.Modules
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.server.PublicAddress
import ratpack.server.internal.ConstantPublicAddress
import spock.lang.Specification
import uk.q3c.build.gitplus.GitPlusModule
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.simplycd.agent.app.ApiModule
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.build.*
import uk.q3c.simplycd.agent.eventbus.GlobalBusModule
import uk.q3c.simplycd.agent.i18n.I18NModule
import uk.q3c.simplycd.agent.i18n.TaskResultStateKey
import uk.q3c.simplycd.agent.lifecycle.LifecycleModule
import uk.q3c.simplycd.agent.project.Project
import uk.q3c.simplycd.agent.project.ProjectModule
import uk.q3c.simplycd.agent.project.Projects
import uk.q3c.simplycd.agent.queue.QueueModule
import uk.q3c.simplycd.agent.queue.RequestQueue
import uk.q3c.simplycd.agent.system.DefaultInstallationInfo
import uk.q3c.simplycd.agent.system.InstallationInfo
import uk.q3c.simplycd.agent.system.SystemModule

import java.time.LocalDateTime
/**
 * Integrates RequestQueue, BuildRunner and GradleExecutor
 *
 * Created by David Sowerby on 08 Jan 2017
 */
class QueueAndBuildIntegrationTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp
    QueueMessageReceiver queueMessageReceiver
    BuildRecordCollator buildResultCollator
    InstallationInfo installationInfo
    class TestSystemModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(PublicAddress.class).toInstance(new ConstantPublicAddress(new URI(ConstantsKt.baseUrl)))
            bind(InstallationInfo.class).toInstance(installationInfo)
        }
    }

    RequestQueue queue
    Projects projects


    def setup() {
        temp = temporaryFolder.getRoot()
        installationInfo = new DefaultInstallationInfo()
        installationInfo.dataDirRoot = temp

        List<Module> bindings = new ArrayList<>()
        bindings.add(new GlobalBusModule())
        bindings.add(new BuildModule())
        bindings.add(Modules.override(new SystemModule()).with(new TestSystemModule()))
        bindings.add(new QueueModule())
        bindings.add(new LifecycleModule())
        bindings.add(new GitPlusModule())
        bindings.add(new I18NModule())
        bindings.add(new ProjectModule())
        bindings.add(new ApiModule())


        Injector injector = Guice.createInjector(bindings)
        queue = injector.getInstance(RequestQueue)
        queueMessageReceiver = injector.getInstance(QueueMessageReceiver)
        buildResultCollator = injector.getInstance(BuildRecordCollator)
        projects = injector.getInstance(Projects.class)


    }

    def cleanup() {
    }

    def "Good build"() {
        given:
        final String projectFullName = 'davidsowerby/simplycd-test'
        final String commitId = "7c3a779e17d65ec255b4c7d40b14950ea6ce232e"
        Project project = projects.getProject(projectFullName)
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20)

        when:
        UUID uid = queue.addRequest(project, new GitSHA(commitId))

        then:
        while (queueMessageReceiver.buildCompletions.isEmpty() && LocalDateTime.now().isBefore(timeout)) {
            Thread.sleep(100)
        }
        BuildRecord result = buildResultCollator.getRecord(uid)
        BuildRecordValidator validator = new BuildRecordValidator(result)
        boolean valid = validator.validate()
        if (!valid) {
            println validator.errors
        }
        valid
        result.taskResults.size() == 1
        TaskResult taskResult = new BuildRecordWrapper(result).taskResult("Unit_Test")
        taskResult.requestedAt.isAfter(result.requestedAt)
        taskResult.startedAt.isAfter(taskResult.requestedAt)
        taskResult.completedAt.isAfter(taskResult.requestedAt)
        taskResult.outcome == TaskResultStateKey.Task_Successful
        taskResult.task.name() == "Unit_Test"


    }
//    Logger.getRootLogger().getLoggerRepository().resetConfiguration()

}
