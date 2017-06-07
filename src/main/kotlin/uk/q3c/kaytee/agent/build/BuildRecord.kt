package uk.q3c.kaytee.agent.build

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.app.buildRecords
import uk.q3c.kaytee.agent.app.zeroDate
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Build_Configuration
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Not_Applicable
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.BuildStateKey.*
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey.*
import uk.q3c.kaytee.agent.i18n.finalStates
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Collates the results for a build.
 *
 * [tasksResults] is synchronised because parallel tasks could cause contention here.  [state] is also synchronised, but
 * other properties would only be accessed sequentially, and therefore not subject to contention
 *
 * Created by David Sowerby on 13 Jan 2017
 */
@JsonIgnoreProperties("stateLock", "taskLock")
class BuildRecord(uid: UUID, var requestedAt: OffsetDateTime) : HalResourceWithId(uid, buildRecords) {
    var preparationStartedAt: OffsetDateTime = zeroDate
    var preparationCompletedAt: OffsetDateTime = zeroDate
    var buildStartedAt: OffsetDateTime = zeroDate
    var buildCompletedAt: OffsetDateTime = zeroDate
    var state: BuildStateKey = Not_Started
    var causeOfFailure = Not_Applicable
    var failureDescription = ""
    private val stateLock = Any()
    private val taskLock = Any()
    // once parallel tasking enabled, there is a contention risk here.
    val taskResults: MutableMap<TaskKey, TaskResult> = mutableMapOf()

    // initialise with empty results, so data set is always complete
    init {
        for (taskKey in TaskKey.values()) {
            // empty results show tasks as 'not run' - better than null
            taskResults.put(taskKey, TaskResult(taskKey, zeroDate))
        }
    }


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
            return state == Successful
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
    fun hasCompleted(): Boolean {
        return finalStates.contains(state)
    }

    fun updateTaskOutcome(taskKey: TaskKey, time: OffsetDateTime, outcome: TaskResultStateKey, stdOut: String = "", stdErr: String = "") {
        synchronized(taskLock) {
            val taskResult = taskResult(taskKey)
            taskResult.completedAt = time
            taskResult.outcome = outcome
            taskResult.stdErr = stdErr
            taskResult.stdOut = stdOut
        }
    }

    override fun toString(): String {
        return summary()
    }

    fun summary(): String {
        val sortedResults = taskResults.values.sortedBy { it.startedAt }
        val buf = StringBuilder()
        for (result in sortedResults) {
            if (!result.notRun()) {
                buf.append(result.summary())
                buf.append("\n")
            }
        }
        return buf.toString()
    }


}

class InvalidTaskException(task: TaskKey) : RuntimeException(task.name)


data class TaskResult(val task: TaskKey, val requestedAt: OffsetDateTime) {
    var completedAt: OffsetDateTime = zeroDate
    var outcome: TaskResultStateKey = Task_Not_Run
    var startedAt: OffsetDateTime = zeroDate
    var stdOut: String = ""
    var stdErr: String = ""

    fun notRun(): Boolean {
        return outcome == Task_Not_Run
    }

    fun failed(): Boolean {
        return outcome == Task_Failed || outcome == Quality_Gate_Failed
    }

    fun cancelled(): Boolean {
        return outcome == Task_Cancelled
    }

    fun summary(): String {
        val units = ChronoUnit.SECONDS
        val elapsedTime = units.between(startedAt, completedAt)
        return "${task.name} : $outcome : $elapsedTime secs $stdErr"
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
            Failed -> {
                if (record.causeOfFailure == Build_Configuration) {
                    buildEmpty()
                    noTaskFailed()
                } else {
                    buildNotEmpty()
                    taskFailed()
                }
            }
            Cancelled -> {
                buildNotEmpty()
                taskCancelled()
            }
            Successful -> {
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
        tasksShouldNotBeEmpty()  // there is now a record for every task, whether executed or not
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
        val duration = Duration.between(earlier, later)
        compare(duration.seconds >= 0, "Build:${this.record.uid}:: $laterName must be at same instant or later than $earlierName, but $laterName is $later and $earlierName is $earlier")
    }

    fun tasksShouldBeEmpty() {
        compare(record.taskResults.isEmpty(), "taskResults should be empty")
    }

    fun tasksShouldNotBeEmpty() {
        compare(record.taskResults.isNotEmpty(), "taskResults should not be empty")
    }


}