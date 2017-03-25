import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.build.gitplus.GitPlusModule
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.simplycd.agent.build.BuildModule
import uk.q3c.simplycd.agent.eventbus.GlobalBusModule
import uk.q3c.simplycd.agent.i18n.I18NModule
import uk.q3c.simplycd.agent.lifecycle.LifecycleModule
import uk.q3c.simplycd.agent.project.Project
import uk.q3c.simplycd.agent.project.ProjectModule
import uk.q3c.simplycd.agent.project.Projects
import uk.q3c.simplycd.agent.queue.QueueModule
import uk.q3c.simplycd.agent.queue.RequestQueue
import uk.q3c.simplycd.agent.system.SystemModule

/**
 * Integrates RequestQueue, BuildRunner and GradleExecutor
 *
 * Created by David Sowerby on 08 Jan 2017
 */
class QueueAndBuildFunctionalTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp
    QueueMessageReceiver queueMessageReceiver


    RequestQueue queue
    Projects projects


    def setup() {
        temp = temporaryFolder.getRoot()

        List<Module> bindings = new ArrayList<>()
        bindings.add(new GlobalBusModule())
        bindings.add(new BuildModule())
        bindings.add(new SystemModule())
        bindings.add(new QueueModule())
        bindings.add(new LifecycleModule())
        bindings.add(new GitPlusModule())
        bindings.add(new I18NModule())
        bindings.add(new ProjectModule())

        Injector injector = Guice.createInjector(bindings)
        queue = injector.getInstance(RequestQueue)
        queueMessageReceiver = injector.getInstance(QueueMessageReceiver)
        projects = injector.getInstance(Projects.class)
    }

    def cleanup() {
    }

    def "Good build"() {
        given:
        final String projectFullName = 'davidsowerby/simplycd-test'
        final String commitId = "7c3a779e17d65ec255b4c7d40b14950ea6ce232e"
        Project project = projects.getProject(projectFullName)
//        Logger logger = LogManager.getRootLogger()

        when:
        queue.addRequest(project, new GitSHA(commitId))

        then:
        while (queueMessageReceiver.buildCompletions.isEmpty()) {
            Thread.sleep(100)
        }
        true //TODO result should be returned and checked


    }
//    Logger.getRootLogger().getLoggerRepository().resetConfiguration()

}
