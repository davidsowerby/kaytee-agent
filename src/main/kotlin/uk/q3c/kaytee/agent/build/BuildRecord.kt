package uk.q3c.kaytee.agent.build

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import uk.q3c.kaytee.agent.app.buildRecords
import uk.q3c.kaytee.agent.app.zeroDate
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Not_Applicable
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.BuildStateKey.Not_Started
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.i18n.TaskStateKey.Not_Run
import uk.q3c.kaytee.agent.i18n.TaskStateKey.Quality_Gate_Failed
import uk.q3c.kaytee.agent.i18n.finalStates
import uk.q3c.kaytee.plugin.TaskKey
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
    /**
     * The time the preparation started, regardless of whether it passed or failed
     */
    var preparationStartedAt: OffsetDateTime = zeroDate
    /**
     * The time the preparation finished, regardless of whether it passed or failed
     */
    var preparationCompletedAt: OffsetDateTime = zeroDate
    /**
     * The time the build itself starts, after preparation completed.  It is actually the time that the first build Task
     * is put into the build queue, so if there is a significant delay in starting the first task, it may be better to use
     * the first task start as a measure of when the build activity starts
     */
    var startedAt: OffsetDateTime = zeroDate
    /**
     * The time the build finished, regardless of whether it passed or failed
     */
    var completedAt: OffsetDateTime = zeroDate
    var state: BuildStateKey = Not_Started
    var causeOfFailure = Not_Applicable
    var failureDescription = ""
    var delegated: Boolean = false
    private val stateLock = Any()
    private val taskLock = Any()
    // once parallel tasking enabled, there is a contention risk here.
    val taskResults: MutableMap<TaskKey, TaskResult> = mutableMapOf()

    // initialise with empty results, so data set is always complete
    init {
        for (taskKey in TaskKey.values()) {
            // empty results show tasks as 'not run' - better than null
            taskResults.put(taskKey, TaskResult(taskKey))
        }
    }


    fun taskResult(task: TaskKey): TaskResult {
        synchronized(taskLock) {
            val taskResult = taskResults[task]
            if (taskResult == null) {
                throw InvalidTaskException(task) // theoretically impossible as all task results created in init block
            } else {
                return taskResult
            }
        }
    }


    fun passed(): Boolean {
        synchronized(stateLock) {
            return state == BuildStateKey.Successful
        }
    }

    fun failed(): Boolean {
        return !passed()
    }

    /**
     * Returns true if the request has been completed - this only means that all processing that can be done has been, the build itself could have failed.
     * To be sure a build was successful, use [passed]
     */
    fun hasCompleted(): Boolean {
        return finalStates.contains(state)
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


data class TaskResult(val task: TaskKey) {
    var requestedAt: OffsetDateTime = zeroDate
    var completedAt: OffsetDateTime = zeroDate
    var state: TaskStateKey = Not_Run
    var startedAt: OffsetDateTime = zeroDate
    var stdOut: String = ""
    var stdErr: String = ""

    fun notRun(): Boolean {
        return state == Not_Run
    }

    fun failed(): Boolean {
        return state == TaskStateKey.Failed || state == Quality_Gate_Failed
    }

    fun cancelled(): Boolean {
        return state == TaskStateKey.Cancelled
    }

    fun summary(): String {
        val units = ChronoUnit.SECONDS
        val elapsedTime = units.between(startedAt, completedAt)
        return "${task.name} : $state : $elapsedTime secs $stdErr"
    }

    fun passed(): Boolean {
        return state == TaskStateKey.Successful
    }
}

