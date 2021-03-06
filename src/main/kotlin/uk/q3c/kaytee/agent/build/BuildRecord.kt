package uk.q3c.kaytee.agent.build

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.app.buildRecords
import uk.q3c.kaytee.agent.app.delegatedLifecycle
import uk.q3c.kaytee.agent.app.standardLifecycle
import uk.q3c.kaytee.agent.app.zeroDate
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Not_Applicable
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.BuildStateKey.Complete
import uk.q3c.kaytee.agent.i18n.BuildStateKey.Not_Started
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.i18n.TaskStateKey.*
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
class BuildRecord(uid: UUID, var requestedAt: OffsetDateTime, val delegated: Boolean) : HalResourceWithId(uid, buildRecords) {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
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
    var processCompletedAt: OffsetDateTime = zeroDate
    var state: BuildStateKey = Not_Started
    var causeOfFailure = Not_Applicable
    var outcome = Not_Started
    var failureDescription = ""
    var failedTask: TaskKey = TaskKey.Custom // valid only if a task has failed
    /**
     * empty unless exception thrown, and holds only root cause stacktrace
     */
    var stacktrace = ""
    private val stateLock = Any()
    private val taskLock = Any()
    // once parallel tasking enabled, there is a contention risk here.
    val taskResults: MutableMap<TaskKey, TaskResult> = mutableMapOf()

    // initialise with empty results, so data set is always complete
    init {
        val lifecycle = if (delegated) {
            delegatedLifecycle
        } else {
            standardLifecycle
        }
        for (taskKey in lifecycle) {
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
            return outcome == BuildStateKey.Successful
        }
    }

    fun failed(): Boolean {
        return !passed()
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

    fun hasCompleted(): Boolean {
        return state == Complete
    }


}

class InvalidTaskException(task: TaskKey, msg: String = "") : RuntimeException("${task.name}: $msg")


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

    fun notRequired(): Boolean {
        return state == Not_Required
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

    fun hasCompleted(): Boolean {
        return state != Not_Run && state != Started && state != Requested
    }
}

