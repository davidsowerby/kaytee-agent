package uk.q3c.kaytee.agent.build

import com.google.common.collect.ImmutableList
import com.google.inject.Provider
import net.engio.mbassy.bus.IMessagePublication
import net.engio.mbassy.bus.MBassador
import org.apache.commons.codec.digest.DigestUtils
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.prepare.PreparationStage
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.kaytee.plugin.TaskKey

import static uk.q3c.kaytee.plugin.TaskKey.*

/**
 * Created by David Sowerby on 09 Jul 2017
 */
class DefaultBuildTest extends Specification {

    Build build
    BuildRunner buildRunner = Mock()
    PreparationStage preparationStage = Mock()
    BuildNumberReader buildNumberReader = Mock()
    RequestQueue requestQueue = Mock()
    GlobalBusProvider globalBusProvider = Mock()
    MockGradleTaskRunnerFactory gradleTaskRunnerFactory = new MockGradleTaskRunnerFactory()
    MockManualTaskRunnerFactory manualTaskRunnerFactory = new MockManualTaskRunnerFactory()
    Provider<IssueCreator> issueCreatorProvider = Mock()
    IssueCreator issueCreator = Mock()
    MockDelegatedProjectTaskRunnerFactory delegatedProjectTaskRunnerFactory = new MockDelegatedProjectTaskRunnerFactory()
    KayTeeExtension kayTeeExtension
    UUID uid = UUID.randomUUID()
    MBassador<BusMessage> globalBus = Mock()
    Project project = Mock()
    List<TaskKey> defaultLifecycle = ImmutableList.of(Unit_Test, Generate_Build_Info, Generate_Change_Log, Publish_to_Local, Merge_to_Master, Tag, Bintray_Upload)
    IMessagePublication messagePublication = Mock()

    void setup() {
        globalBusProvider.get() >> globalBus
        issueCreatorProvider.get() >> issueCreator
        kayTeeExtension = new KayTeeExtension()
        build = new DefaultBuild(preparationStage, buildNumberReader, requestQueue, globalBusProvider, gradleTaskRunnerFactory, manualTaskRunnerFactory, delegatedProjectTaskRunnerFactory, issueCreatorProvider, buildRunner)
        buildRunner.uid >> uid
        buildRunner.project >> project
        project.fullProjectName >> 'davidsowerby/wiggly'

    }

    void cleanup() {
    }


    def "configure, default"() {

        when:
        build.configure(kayTeeExtension)

        then:
        build.raiseIssueOnFail
        build.lifecycle == ConstantsKt.standardLifecycle
        build.tasksWaiting() == defaultLifecycle
        !gradleTaskRunnerFactory.runners.get(Unit_Test).includeQualityGate

    }

    def "configure, modified default"() {
        given:
        kayTeeExtension.unitTest.enabled = false
        kayTeeExtension.generateChangeLog = false
        kayTeeExtension.release.mergeToMaster = false
        kayTeeExtension.functionalTest.enabled = true
        kayTeeExtension.functionalTest.qualityGate = true

        when:
        build.configure(kayTeeExtension)

        then:
        build.raiseIssueOnFail
        build.lifecycle == ConstantsKt.standardLifecycle
        build.tasksWaiting() == ImmutableList.of(Generate_Build_Info, Publish_to_Local, Functional_Test, Tag, Bintray_Upload)
        gradleTaskRunnerFactory.runners.get(Functional_Test).includeQualityGate

    }

    def "configure, delegated build"() {
        given:
        buildRunner.delegated >> true
        kayTeeExtension.raiseIssueOnFail = false

        when:
        build.configure(kayTeeExtension)

        then:
        !build.raiseIssueOnFail
        build.lifecycle == ConstantsKt.delegatedLifecycle
        build.tasksWaiting() == ImmutableList.of(Custom)

    }

    def "default config, execute all pass"() {
        given:
        build.configure(kayTeeExtension)
        buildRunner.delegated >> false


        when:
        build.execute()

        then:
        1 * preparationStage.execute(build)

        then: "first task added to main queue, removed from holding queue"
        1 * requestQueue.addRequest(gradleTaskRunnerFactory.runners.get(Unit_Test))
        !build.tasksWaiting().contains(Unit_Test)

        then: "BuildStartedMessage sent"
        1 * buildNumberReader.nextBuildNumber(build) >> '33'
        1 * globalBus.publishAsync(new BuildStartedMessage(uid, false, '33')) >> messagePublication
        build.buildNumber() == '33'

        when: "first TaskSuccessful message received"
        TaskSuccessfulMessage message = new TaskSuccessfulMessage(uid, Unit_Test, false, "")
        DefaultBuild b = build as DefaultBuild
        b.taskCompleted(message)


        then: "completed tasks added to, next task to main queue"
        b.completedTasks.contains(Unit_Test)
        1 * requestQueue.addRequest(gradleTaskRunnerFactory.runners.get(Generate_Build_Info))
        !build.tasksWaiting().contains(Generate_Build_Info)

        when: "other tasks complete successfully"
        b.taskCompleted(new TaskSuccessfulMessage(uid, Generate_Build_Info, false, ""))
        b.taskCompleted(new TaskSuccessfulMessage(uid, Generate_Change_Log, false, ""))
        b.taskCompleted(new TaskSuccessfulMessage(uid, Publish_to_Local, false, ""))
        b.taskCompleted(new TaskSuccessfulMessage(uid, Merge_to_Master, false, ""))
        b.taskCompleted(new TaskSuccessfulMessage(uid, Tag, false, ""))
        b.taskCompleted(new TaskSuccessfulMessage(uid, Bintray_Upload, false, ""))

        then: "all tasks complete, BuildSuccessfulMessage sent"
        build.tasksWaiting().isEmpty()
        b.completedTasks.containsAll(defaultLifecycle)
        1 * globalBus.publishAsync(new BuildSuccessfulMessage(uid, false)) >> messagePublication

        then:
        1 * globalBus.publishAsync(new BuildProcessCompletedMessage(uid, false)) >> messagePublication
    }

    def "delegated task passes"() {
        given:
        buildRunner.delegated >> true
        build.configure(kayTeeExtension)


        when:
        build.execute()

        then:
        1 * preparationStage.execute(build)

        then: "first task added to main queue, removed from holding queue"
        1 * requestQueue.addRequest(gradleTaskRunnerFactory.runners.get(Custom))
        !build.tasksWaiting().contains(Custom)

        then: "BuildStartedMessage sent"
        1 * buildNumberReader.nextBuildNumber(build) >> '33'
        1 * globalBus.publishAsync(new BuildStartedMessage(uid, true, '33')) >> messagePublication

        when: "first TaskSuccessful message received"
        TaskSuccessfulMessage message = new TaskSuccessfulMessage(uid, Custom, true, "")
        DefaultBuild b = build as DefaultBuild
        b.taskCompleted(message)


        then: "all tasks complete, BuildSuccessfulMessage sent"
        build.tasksWaiting().isEmpty()
        b.completedTasks.containsAll(ConstantsKt.delegatedLifecycle)
        1 * globalBus.publishAsync(new BuildSuccessfulMessage(uid, true)) >> messagePublication

        then:
        1 * globalBus.publishAsync(new BuildProcessCompletedMessage(uid, true)) >> messagePublication
    }

    @Unroll
    def "build fails, delegated is: #delegated , raiseIssue is: #raiseIssueCount"() {
        given:

        buildRunner.delegated >> delegated
        kayTeeExtension.raiseIssueOnFail = raiseIssueCount == 1
        build.configure(kayTeeExtension)
        BuildMessage buildMessage
        IMessagePublication messagePublication = Mock()

        when:
        build.execute()
        DefaultBuild b = build as DefaultBuild
        b.taskCompleted(new TaskFailedMessage(uid, failedTask, delegated, TaskStateKey.Failed, "stdout", "stderr"))

        then:
        1 * buildNumberReader.nextBuildNumber(build) >> '33'

        then:

        1 * globalBus.publishAsync(_ as BuildStartedMessage) >> { arguments ->
            buildMessage = arguments[0]
            return messagePublication
        }

        then:

        buildMessage.buildRequestId == uid
        1 * globalBus.publishAsync(_ as BuildFailedMessage) >> { arguments ->
            buildMessage = arguments[0]
            return messagePublication
        }
        buildMessage.buildRequestId == uid
        raiseIssueCount * issueCreator.raiseIssue(build)

        then:

        1 * globalBus.publishAsync(_ as BuildProcessCompletedMessage) >> { arguments ->
            buildMessage = arguments[0]
            return messagePublication
        }
        buildMessage.buildRequestId == uid

        where:
        delegated | failedTask | raiseIssueCount
        true      | Custom     | 0
        true      | Custom     | 1
        false     | Unit_Test  | 0
        false     | Unit_Test  | 1


    }

    @Ignore("Cannot do this test until it is possible to disable PublishToMavenLocal")
    def "config has all tasks disabled, throw BuildConfigurationException"() {
        given:
        kayTeeExtension.unitTest.enabled = false
        kayTeeExtension.release.mergeToMaster = false
        kayTeeExtension.release.toBintray = false
        kayTeeExtension.versionTag = false
        kayTeeExtension.generateChangeLog = false
        kayTeeExtension.generateBuildInfo = false
//        kayTeeExtension.publishToMavenLocal = false

        when:
        build.configure(kayTeeExtension)

        then:
        build.tasksWaiting().isEmpty()

        when:
        build.execute()

        then:
        thrown BuildConfigurationException
    }

    def "preparation fails, PreparationFailedMessage and BuildFailedMessage sent"() {
        given:
        build.configure(kayTeeExtension)
        buildRunner.delegated >> false


        when:
        build.execute()

        then:
        1 * preparationStage.execute(build) >> { throw new IOException("Fake") }
        1 * globalBus.publishAsync(_ as BuildFailedMessage) >> messagePublication
        1 * globalBus.publishAsync(new BuildProcessCompletedMessage(uid, false)) >> messagePublication
    }


    def "create mix of gradle, delegated and manual tasks"() {
        given:
        kayTeeExtension.functionalTest.manual = true
        kayTeeExtension.functionalTest.enabled = true
        kayTeeExtension.functionalTest.auto = false
        kayTeeExtension.acceptanceTest.delegated = true
        kayTeeExtension.acceptanceTest.enabled = true
        kayTeeExtension.acceptanceTest.delegate.commitId = sha(2)
        kayTeeExtension.acceptanceTest.delegate.repoUserName = 'davidsowerby'
        kayTeeExtension.acceptanceTest.delegate.repoName = 'wiggly'

        buildRunner.delegated >> false


        when:
        build.configure(kayTeeExtension)

        then:
        build.tasksWaiting().containsAll(ImmutableList.of(Unit_Test, Generate_Build_Info, Generate_Change_Log, Publish_to_Local, Functional_Test, Acceptance_Test, Merge_to_Master, Tag, Bintray_Upload))
        delegatedProjectTaskRunnerFactory.runners.containsKey(Acceptance_Test)
        manualTaskRunnerFactory.runners.containsKey(Functional_Test)
    }


    private String sha(int i) {
        return DigestUtils.sha1Hex('x').toString()
    }
}
