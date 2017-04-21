package uk.q3c.simplycd.agent.build

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.app.buildRecords
import uk.q3c.simplycd.agent.app.zeroDate
import uk.q3c.simplycd.agent.i18n.BuildFailCauseKey.Build_Configuration
import uk.q3c.simplycd.agent.i18n.BuildFailCauseKey.Not_Applicable
import uk.q3c.simplycd.agent.i18n.BuildStateKey
import uk.q3c.simplycd.agent.i18n.BuildStateKey.*
import uk.q3c.simplycd.agent.i18n.TaskKey
import uk.q3c.simplycd.agent.i18n.TaskResultStateKey
import uk.q3c.simplycd.agent.i18n.finalStates
import java.time.OffsetDateTime
import java.util.*

/**
 * Collates the results for a build.
 *
 * [tasksResults] is synchronised because parallel tasks could cause contention here.  [state] is also synchronised, but
 * other properties would only be accessed sequentially, and therefore not be subject to contention
 *
 * Created by David Sowerby on 13 Jan 2017
 */
@JsonIgnoreProperties("stateLock", "taskLock")
class BuildRecord(uid: UUID, val requestedAt: OffsetDateTime) : HalResourceWithId(uid, buildRecords) {
    var preparationStartedAt: OffsetDateTime = zeroDate
    var preparationCompletedAt: OffsetDateTime = zeroDate
    var buildStartedAt: OffsetDateTime = zeroDate
    var buildCompletedAt: OffsetDateTime = zeroDate
    var state: BuildStateKey = Not_Started
    var causeOfFailure = Not_Applicable
    private val stateLock = Any()
    private val taskLock = Any()
    // once parallel tasking enabled, contention risk
    val taskResults: MutableMap<TaskKey, TaskResult> = mutableMapOf()


    fun addTask(task: TaskKey, time: OffsetDateTime) {
        synchronized(taskLock) {
            val taskResult = TaskResult(task, time)
            taskResults.put(task, taskResult)
        }
    }

    fun validate(): Boolean {
        return BuildRecordValidator(this).validate()
    }

    fun taskResult(task: TaskKey): TaskResult {
        synchronized(taskLock) {
            val taskResult = taskResults[task]
            if (taskResult == null) {
                throw InvalidTaskException(task)
            } else {
                return taskResult
            }
        }
    }


    fun passed(): Boolean {
        synchronized(stateLock) {
            return state == Build_Successful
        }
    }

    fun failed(): Boolean {
        return !passed()
    }

    fun updateTaskStart(taskKey: TaskKey, time: OffsetDateTime) {
        synchronized(taskLock) {
            taskResult(taskKey).startedAt = time
        }
    }

    /**
     * Returns true if the request has been completed - this only means that all processing that can be done has been, the build itself could have failed.
     * To be sure a build was successful, use [passed]
     */
    fun requestedCompleted(): Boolean {
        return finalStates.contains(state)
    }

    fun updateTaskOutcome(taskKey: TaskKey, time: OffsetDateTime, outcome: TaskResultStateKey) {
        synchronized(taskLock) {
            val taskResult = taskResult(taskKey)
            taskResult.completedAt = time
            taskResult.outcome = outcome
        }
    }


}

class InvalidTaskException(task: TaskKey) : RuntimeException(task.name)


class TaskResult(val task: TaskKey, val requestedAt: OffsetDateTime) {
    var completedAt: OffsetDateTime = zeroDate
    var outcome: TaskResultStateKey = TaskResultStateKey.Task_Not_Run
    var startedAt: OffsetDateTime = zeroDate

    fun failed(): Boolean {
        return outcome == TaskResultStateKey.Task_Failed
    }

    fun cancelled(): Boolean {
        return outcome == TaskResultStateKey.Task_Cancelled
    }
}

class BuildRecordValidator(val record: BuildRecord) {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    val errors: MutableList<String> = mutableListOf()
    lateinit var validatedAt: BuildStateKey
    fun validate(): Boolean {
        log.debug("Validating {}", record.uid)
        when (record.state) {
            Not_Started -> {
                shouldNotBeSet("preparationStartedAt", record.preparationStartedAt)
                shouldNotBeSet("preparationCompletedAt", record.preparationCompletedAt)
            }
            Preparation_Started -> {
                shouldBeSet("preparationStartedAt", record.preparationStartedAt)
                shouldNotBeSet("preparationCompletedAt", record.preparationCompletedAt)
                buildEmpty()
            }
            Preparation_Successful -> {
                preparation()
                buildEmpty()
            }
            Preparation_Failed -> {
                preparation()
                buildEmpty()
            }
            Build_Started -> {
                preparation()
                shouldBeSet("buildStartedAt", record.buildStartedAt)
                tasksShouldNotBeEmpty()
            }
            Build_Failed -> {
                if (record.causeOfFailure == Build_Configuration) {
                    buildEmpty()
                    noTaskFailed()
                } else {
                    buildNotEmpty()
                    taskFailed()
                }
            }
            Build_Cancelled -> {
                buildNotEmpty()
                taskCancelled()
            }
            Build_Successful -> {
                buildNotEmpty()
                noTaskFailed()
            }
        }
        validatedAt = record.state

        if (errors.isNotEmpty()) {
            log.error("BuildResult validated at '{}', produces errors: \n {}", validatedAt, errors.toString())
        }
        return errors.isEmpty()
    }

    /**
     * No tasks - would only be if preparation failed, or build start failed
     */
    private fun buildEmpty() {
        shouldNotBeSet("buildStartedAt", record.buildStartedAt)
        shouldNotBeSet("buildCompletedAt", record.buildCompletedAt)
        tasksShouldBeEmpty()
    }

    /**
     * At least one task
     */
    private fun buildNotEmpty() {
        preparation()
        shouldBeSet("buildStartedAt", record.buildStartedAt)
        shouldBeSet("buildCompletedAt", record.buildCompletedAt)
        tasksShouldNotBeEmpty()
    }

    /**
     * At least one task has failed
     */
    private fun taskFailed() {
        var noneFailed = true
        for (taskResult in record.taskResults.values) {
            if (taskResult.failed()) {
                noneFailed = false
                break
            }
        }
        if (noneFailed) {
            errors.add("At least one task should be in a 'failed' state when causeOfFailure is: ${record.causeOfFailure}")
        }
    }

    /**
     * No tasks have failed
     */
    private fun noTaskFailed() {
        var noneFailed = true
        for (taskResult in record.taskResults.values) {
            if (taskResult.failed()) {
                noneFailed = false
                break
            }
        }
        if (!noneFailed) {
            errors.add("No tasks should be in a 'failed' state")
        }
    }

    /**
     * At least one task has been cancelled
     */
    private fun taskCancelled() {
        var noneCancelled = true
        for (taskResult in record.taskResults.values) {
            if (taskResult.cancelled()) {
                noneCancelled = false
                break
            }
        }
        if (noneCancelled) {
            errors.add("At least one task should be in a 'cancelled' state")
        }
    }

    /**
     * Build has been requested
     */
    private fun requested() {
        shouldBeSet("requestedAt", record.requestedAt)
    }

    /**
     * Preparation has started, and completed (completion could be failure)
     */
    private fun preparation() {
        requested()
        mustBeLaterOrEqual("preparationStartedAt", "requestedAt", record.preparationStartedAt, record.requestedAt)
        mustBeLaterOrEqual("preparationCompletedAt", "preparationStartedAt", record.preparationCompletedAt, record.preparationStartedAt)
    }

    fun compare(valid: Boolean, failMessage: String) {
        if (!valid) {
            errors.add(failMessage)
        }
    }

    fun shouldNotBeSet(name: String, time: OffsetDateTime) {
        compare(time == zeroDate, "$name should not be set")
    }

    fun shouldBeSet(name: String, time: OffsetDateTime) {
        compare(time.isAfter(zeroDate), "$name should be set")
    }

    fun mustBeLaterOrEqual(laterName: String, earlierName: String, later: OffsetDateTime, earlier: OffsetDateTime) {
        val diff = later.compareTo(earlier)
        compare(diff >= 0, "$laterName must be at same instant or later than $earlierName")
    }

    fun tasksShouldBeEmpty() {
        compare(record.taskResults.isEmpty(), "taskResults should be empty")
    }

    fun tasksShouldNotBeEmpty() {
        compare(record.taskResults.isNotEmpty(), "taskResults should not be empty")
    }


}