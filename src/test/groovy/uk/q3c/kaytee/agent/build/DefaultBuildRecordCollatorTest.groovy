package uk.q3c.kaytee.agent.build

import spock.lang.Specification
import spock.lang.Unroll
import uk.q3c.kaytee.agent.app.Hooks
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.queue.*

import java.time.Duration
import java.time.OffsetDateTime

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.*
import static uk.q3c.kaytee.agent.i18n.BuildStateKey.*
import static uk.q3c.kaytee.plugin.TaskKey.Merge_to_Master

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
        record.taskResults.size() == 12  // constructed at start

        numpty * hooks.publish(_)

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

        numpty * hooks.publish(_)

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

        numpty * hooks.publish(_)

        when: "Build successful"
        collator.busMessage(buildSuccessfulMessage)
        record = collator.getRecord(uid)


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

        numpty * hooks.publish(_)

        where:
        delegated | numpty
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

        (numpty * 4) * hooks.publish(_)

        where:
        delegated | numpty | exceptionMsg
        false     | 1      | "wiggly"
        true      | 0      | null

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

        (numpty * 5) * hooks.publish(_)

        where:
        delegated | numpty
        false     | 1
        true      | 0

    }

    def "Task is successful"() {
        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        TaskRequestedMessage taskRequestedMessage = new TaskRequestedMessage(uid, Merge_to_Master, delegated)
        TaskStartedMessage taskStartedMessage = new TaskStartedMessage(uid, Merge_to_Master, delegated)
        TaskSuccessfulMessage taskSuccessfulMessage = new TaskSuccessfulMessage(uid, Merge_to_Master, delegated, stdOut)

        when:
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        collator.busMessage(taskRequestedMessage)
        TaskResult taskRecord = collator.getRecord(uid).taskResult(Merge_to_Master)

        then:
        taskRecord.state == TaskStateKey.Requested
        taskRecord.stdOut == ""
        taskRecord.stdErr == ""

        isSet(taskRecord.requestedAt)
        isNotSet(taskRecord.startedAt)
        isNotSet(taskRecord.completedAt)

        when:
        collator.busMessage(taskStartedMessage)

        then:
        taskRecord.state == TaskStateKey.Started
        taskRecord.stdOut == ""
        taskRecord.stdErr == ""

        isSet(taskRecord.requestedAt)
        isSet(taskRecord.startedAt)
        isNotSet(taskRecord.completedAt)

        when:
        collator.busMessage(taskSuccessfulMessage)

        then:
        taskRecord.state == TaskStateKey.Successful
        taskRecord.stdOut == stdOut
        taskRecord.stdErr == ""

        isSet(taskRecord.requestedAt)
        isSet(taskRecord.startedAt)
        isSet(taskRecord.completedAt)

        numpty * hooks.publish(_)


        where:
        delegated | numpty
        false     | 1
        true      | 0
    }

    def "Task fails, quality gate broken"() {
        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        TaskRequestedMessage taskRequestedMessage = new TaskRequestedMessage(uid, Merge_to_Master, delegated)
        TaskStartedMessage taskStartedMessage = new TaskStartedMessage(uid, Merge_to_Master, delegated)
        TaskFailedMessage taskFailedMessage = new TaskFailedMessage(uid, Merge_to_Master, delegated, TaskStateKey.Quality_Gate_Failed, stdOut, stdErr)

        when:
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        collator.busMessage(taskRequestedMessage)
        collator.busMessage(taskStartedMessage)
        collator.busMessage(taskFailedMessage)
        BuildRecord buildRecord = collator.getRecord(uid)
        TaskResult taskRecord = buildRecord.taskResult(Merge_to_Master)

        then:
        taskRecord.state == TaskStateKey.Quality_Gate_Failed
        taskRecord.stdOut == stdOut
        taskRecord.stdErr == stdErr

        isSet(taskRecord.requestedAt)
        isSet(taskRecord.startedAt)
        isSet(taskRecord.completedAt)

        buildRecord.failureDescription == stdOut



        where:
        delegated | calls
        false     | 1
        true      | 0
    }

    def "Task not required"() {
        given:
        BuildQueuedMessage queuedMessage = new BuildQueuedMessage(uid, delegated)
        BuildStartedMessage startedMessage = new BuildStartedMessage(uid, delegated, "0.0")
        TaskNotRequiredMessage taskNotRequiredMessage = new TaskNotRequiredMessage(uid, Merge_to_Master, delegated)
        TaskStartedMessage taskStartedMessage = new TaskStartedMessage(uid, Merge_to_Master, delegated)
        TaskFailedMessage taskFailedMessage = new TaskFailedMessage(uid, Merge_to_Master, delegated, TaskStateKey.Quality_Gate_Failed, stdOut, stdErr)

        when:
        collator.busMessage(queuedMessage)
        collator.busMessage(startedMessage)
        collator.busMessage(taskNotRequiredMessage)
        BuildRecord buildRecord = collator.getRecord(uid)
        TaskResult taskRecord = buildRecord.taskResult(Merge_to_Master)

        then:
        taskRecord.state == TaskStateKey.Not_Required
        taskRecord.stdOut == ""
        taskRecord.stdErr == ""

        isNotSet(taskRecord.requestedAt)
        isNotSet(taskRecord.startedAt)
        isNotSet(taskRecord.completedAt)

        buildRecord.failureDescription == ""

        when:
        collator.busMessage(taskStartedMessage)

        then:
        thrown InvalidBuildStateException



        where:
        delegated | calls
        false     | 1
        true      | 0
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
