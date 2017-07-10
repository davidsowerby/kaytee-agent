package uk.q3c.kaytee.agent.build

import spock.lang.Specification
import spock.lang.Unroll
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.app.Hooks
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.queue.*

import java.time.Duration
import java.time.OffsetDateTime

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.*
import static uk.q3c.kaytee.agent.i18n.BuildStateKey.*
import static uk.q3c.kaytee.plugin.TaskKey.*

/**
 * Created by David Sowerby on 11 Jun 2017
 */
class DefaultBuildRecordCollatorTest extends Specification {

    DefaultBuildRecordCollator collator
    Hooks hooks = Mock(Hooks)
    static UUID uid
    BuildRecord record
    final String stdOut = "stdOut"
    final String stdErr = "stdErr"


    def setupSpec() {
        uid = UUID.randomUUID()
    }

    def setup() {
        collator = new DefaultBuildRecordCollator(hooks)

    }

    @Unroll
    def "successful build, delegated is #delegated"() {

        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        PreparationStartedMessage preparationStartedMessage = new PreparationStartedMessage(uid, delegated)
        PreparationSuccessfulMessage preparationSuccessfulMessage = new PreparationSuccessfulMessage(uid, delegated)
        BuildSuccessfulMessage buildSuccessfulMessage = new BuildSuccessfulMessage(uid, delegated)
        BuildProcessCompletedMessage buildCompletedMessage = new BuildProcessCompletedMessage(uid, delegated)
        OffsetDateTime completedAt

        when: "Queued"
        collator.busMessage(queuedMessage)
        record = collator.getRecord(uid)

        then:
        record.state == Requested

        isSet(record.requestedAt)
        isNotSet(record.startedAt)
        isNotSet(record.preparationStartedAt)
        isNotSet(record.preparationCompletedAt)
        isNotSet(record.completedAt)

        record.delegated == delegated
        record.causeOfFailure == Not_Applicable
        record.failureDescription == ""
        record.taskResults.size() == delegated ? 1 : ConstantsKt.standardLifecycle.size()  // constructed at start

        msgsPublished * hooks.publish(_)

        when: "Started"
        collator.busMessage(startedMessage)
        record = collator.getRecord(uid)

        then:
        record.state == Started

        isSet(record.requestedAt)
        isSet(record.startedAt)
        isNotSet(record.preparationStartedAt)
        isNotSet(record.preparationCompletedAt)
        isNotSet(record.completedAt)

        record.delegated == delegated
        record.causeOfFailure == Not_Applicable
        record.failureDescription == ""

        msgsPublished * hooks.publish(_)

        when: "Started prep"
        collator.busMessage(preparationStartedMessage)
        record = collator.getRecord(uid)


        then:
        record.state == Preparation_Started

        isSet(record.requestedAt)
        isSet(record.startedAt)
        isSet(record.preparationStartedAt)
        isNotSet(record.preparationCompletedAt)
        isNotSet(record.completedAt)

        record.delegated == delegated
        record.causeOfFailure == Not_Applicable
        record.failureDescription == ""

        when: "Prep successful"
        collator.busMessage(preparationSuccessfulMessage)
        record = collator.getRecord(uid)


        then:
        record.state == Preparation_Successful

        isSet(record.requestedAt)
        isSet(record.startedAt)
        isSet(record.preparationStartedAt)
        isSet(record.preparationCompletedAt)
        isNotSet(record.completedAt)

        record.delegated == delegated
        record.causeOfFailure == Not_Applicable
        record.failureDescription == ""

        msgsPublished * hooks.publish(_)

        when: "Build successful"
        collator.busMessage(buildSuccessfulMessage)
        record = collator.getRecord(uid)
        completedAt = record.completedAt


        then:
        record.state == Successful

        isSet(record.requestedAt)
        isSet(record.startedAt)
        isSet(record.preparationStartedAt)
        isSet(record.preparationCompletedAt)
        isSet(record.completedAt)



        record.delegated == delegated
        record.causeOfFailure == Not_Applicable
        record.failureDescription == ""

        msgsPublished * hooks.publish(_)
        !record.processingCompleted


        when: "Build processing completed"
        collator.busMessage(buildCompletedMessage)
        record = collator.getRecord(uid)

        then:
        record.processingCompleted
        record.completedAt == completedAt // completion time should remain unchanged

        where:
        delegated | msgsPublished
        false     | 1
        true      | 0

    }

    @Unroll
    def "Preparation fails, delegated is #delegated"() {

        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        PreparationStartedMessage preparationStartedMessage = new PreparationStartedMessage(uid, delegated)
        PreparationFailedMessage preparationFailedMessage = new PreparationFailedMessage(uid, delegated, new IllegalArgumentException(exceptionMsg as String))

        when: "Queued, Build and Prep started, prep fails"
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        collator.busMessage(preparationStartedMessage)
        collator.busMessage(preparationFailedMessage)
        record = collator.getRecord(uid)


        then:
        record.state == Failed

        isSet(record.requestedAt)
        isSet(record.startedAt)
        isSet(record.preparationStartedAt)
        isSet(record.preparationCompletedAt)
        isSet(record.completedAt)

        record.delegated == delegated
        record.causeOfFailure == Preparation_Failed
        if (exceptionMsg == null) {
            record.failureDescription == "IllegalArgumentException"
        } else {
            record.failureDescription == "wiggly"
        }

        (msgsPublished * 4) * hooks.publish(_)

        where:
        delegated | msgsPublished | exceptionMsg
        false     | 1             | "wiggly"
        true      | 0             | null

    }

    @Unroll
    def "Build fails, delegated is #delegated"() {

        given:
        String eMsg2 = "Wiggly just a small beast"
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        PreparationStartedMessage preparationStartedMessage = new PreparationStartedMessage(uid, delegated)
        PreparationSuccessfulMessage preparationSuccessfulMessage = new PreparationSuccessfulMessage(uid, delegated)
        BuildFailedMessage buildFailedMessage = new BuildFailedMessage(uid, delegated, new BuildConfigurationException())

        when: "Queued, Build and Prep started, prep fails"
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        collator.busMessage(preparationStartedMessage)
        collator.busMessage(preparationSuccessfulMessage)
        collator.busMessage(buildFailedMessage)
        record = collator.getRecord(uid)


        then:
        record.state == Failed

        isSet(record.requestedAt)
        isSet(record.startedAt)
        isSet(record.preparationStartedAt)
        isSet(record.preparationCompletedAt)
        isSet(record.completedAt)

        record.delegated == delegated
        record.causeOfFailure == Build_Configuration
        record.failureDescription.contains("There were no tasks to carry out")

        (msgsPublished * 5) * hooks.publish(_)

        where:
        delegated | msgsPublished
        false     | 1
        true      | 0

    }

    @Unroll
    def "Task is successful, delegated is #delegated"() {
        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        TaskRequestedMessage taskRequestedMessage = new TaskRequestedMessage(uid, taskKey, delegated)
        TaskStartedMessage taskStartedMessage = new TaskStartedMessage(uid, taskKey, delegated)
        TaskSuccessfulMessage taskSuccessfulMessage = new TaskSuccessfulMessage(uid, taskKey, delegated, stdOut)
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        BuildRecord buildRecord = collator.getRecord(uid)
        TaskResult taskRecord = buildRecord.taskResult(taskKey)

        when:
        collator.busMessage(taskRequestedMessage)


        then:
        taskRecord.state == TaskStateKey.Requested
        taskRecord.stdOut == ""
        taskRecord.stdErr == ""

        isSet(taskRecord.requestedAt)
        isNotSet(taskRecord.startedAt)
        isNotSet(taskRecord.completedAt)

        msgsPublished * hooks.publish(buildRecord)

        when:
        collator.busMessage(taskStartedMessage)

        then:
        taskRecord.state == TaskStateKey.Started
        taskRecord.stdOut == ""
        taskRecord.stdErr == ""

        isSet(taskRecord.requestedAt)
        isSet(taskRecord.startedAt)
        isNotSet(taskRecord.completedAt)

        msgsPublished * hooks.publish(collator.getRecord(uid))

        when:
        collator.busMessage(taskSuccessfulMessage)

        then:
        taskRecord.state == TaskStateKey.Successful
        taskRecord.stdOut == stdOut
        taskRecord.stdErr == ""

        isSet(taskRecord.requestedAt)
        isSet(taskRecord.startedAt)
        isSet(taskRecord.completedAt)

        msgsPublished * hooks.publish(collator.getRecord(uid))


        where:
        delegated | taskKey         | msgsPublished
        false     | Merge_to_Master | 1
        true      | Custom          | 0
    }

    @Unroll
    def "Task fails, quality gate broken delegated is #delegated"() {
        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        TaskRequestedMessage taskRequestedMessage = new TaskRequestedMessage(uid, taskKey, delegated)
        TaskStartedMessage taskStartedMessage = new TaskStartedMessage(uid, taskKey, delegated)
        TaskFailedMessage taskFailedMessage = new TaskFailedMessage(uid, taskKey, delegated, TaskStateKey.Quality_Gate_Failed, stdOut, stdErr)

        when:
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        collator.busMessage(taskRequestedMessage)
        collator.busMessage(taskStartedMessage)
        collator.busMessage(taskFailedMessage)
        BuildRecord buildRecord = collator.getRecord(uid)
        TaskResult taskRecord = buildRecord.taskResult(taskKey)

        then:
        taskRecord.state == TaskStateKey.Quality_Gate_Failed
        taskRecord.stdOut == stdOut
        taskRecord.stdErr == stdErr

        isSet(taskRecord.requestedAt)
        isSet(taskRecord.startedAt)
        isSet(taskRecord.completedAt)

        buildRecord.failureDescription == stdOut



        where:
        delegated | calls | taskKey
        false     | 1     | Unit_Test
        true      | 0     | Custom
    }

    def "Task not required in standard lifecycle"() {
        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        TaskNotRequiredMessage taskNotRequiredMessage = new TaskNotRequiredMessage(uid, Merge_to_Master, delegated)
        TaskStartedMessage taskStartedMessage = new TaskStartedMessage(uid, Merge_to_Master, delegated)

        when:
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        collator.busMessage(taskNotRequiredMessage)
        BuildRecord buildRecord = collator.getRecord(uid)
        TaskResult taskRecord = buildRecord.taskResult(Merge_to_Master)

        then: "has the correct state"
        taskRecord.state == TaskStateKey.Not_Required
        taskRecord.stdOut == ""
        taskRecord.stdErr == ""

        isNotSet(taskRecord.requestedAt)
        isNotSet(taskRecord.startedAt)
        isNotSet(taskRecord.completedAt)

        buildRecord.failureDescription == ""

        when: "we start a 'not required' task"
        collator.busMessage(taskStartedMessage)

        then: "throw exception as it should not be started"
        thrown InvalidBuildStateException



        where: "cannot make 'delegated' task 'Not_Required', so do not test for it"
        delegated | calls
        false     | 1
    }

    def "getRecord with invalid id throws exception"() {
        when:
        collator.getRecord(UUID.randomUUID())

        then:
        thrown InvalidBuildRequestIdException
    }


    private boolean isSet(OffsetDateTime time) {
        return Duration.between(time, OffsetDateTime.now()).seconds < 2
    }

    private boolean isNotSet(OffsetDateTime time) {
        return Duration.between(time, OffsetDateTime.now()).seconds > 10000
    }


}
