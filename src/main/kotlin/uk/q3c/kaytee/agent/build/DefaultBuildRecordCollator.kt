package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.app.Hooks
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Preparation_Failed
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Task_Failure
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.BuildStateKey.Preparation_Started
import uk.q3c.kaytee.agent.i18n.BuildStateKey.Preparation_Successful
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.i18n.TaskStateKey.*
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.krail.core.eventbus.GlobalBus
import uk.q3c.krail.core.eventbus.SubscribeTo
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Becuasue all the messages handled here are despatched asynchronously, most likely from different threads, there is no
 * guarantee that they will arrive in the order that might be expected.  This is especially true at the beginning of the cycle,
 * where [BuildQueuedMessage], [PreparationStartedMessage] and [BuildStartedMessage] are all sent very soon after each other.
 *
 * For that reason, records are created (if not already existing) by [getRecord] when any message is passed as a parameter
 *
 * Created by David Sowerby on 25 Mar 2017
 */
@Listener @SubscribeTo(GlobalBus::class)
class DefaultBuildRecordCollator @Inject constructor(val hooks: Hooks) : BuildRecordCollator {


    override val records: MutableMap<UUID, BuildRecord> = ConcurrentHashMap()
    val delegateBuildRecords: MutableMap<UUID, BuildRecord> = ConcurrentHashMap()
    private val lock = Any()
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    @Handler
    fun busMessage(busMessage: BuildQueuedMessage) {
        updateBuildState(BuildStateKey.Requested, busMessage)
    }

    @Handler
    fun busMessage(busMessage: BuildStartedMessage) {
        updateBuildState(BuildStateKey.Started, busMessage)
    }

    @Handler
    fun busMessage(busMessage: BuildSuccessfulMessage) {
        updateBuildState(BuildStateKey.Successful, busMessage)
    }


    @Handler
    fun busMessage(busMessage: BuildFailedMessage) {
        val record = updateBuildState(BuildStateKey.Failed, busMessage)
        record.causeOfFailure = BuildExceptionLookup().lookupKeyFromException(busMessage.e)
        record.failureDescription =
                if (busMessage.e.message != null) {
                    busMessage.e.message as String
                } else {
                    busMessage.e.javaClass.simpleName
                }
    }


    @Handler
    fun busMessage(busMessage: PreparationStartedMessage) {
        updateBuildState(Preparation_Started, busMessage)
    }

    @Handler
    fun busMessage(busMessage: PreparationSuccessfulMessage) {
        updateBuildState(Preparation_Successful, busMessage)
    }

    @Handler
    fun busMessage(busMessage: PreparationFailedMessage) {
        val record = updateBuildState(BuildStateKey.Failed, busMessage)
        record.failureDescription =
                if (busMessage.e.message != null) {
                    busMessage.e.message as String
                } else {
                    busMessage.e.javaClass.simpleName
                }
        record.causeOfFailure = Preparation_Failed
        record.preparationCompletedAt = busMessage.time
    }

    @Handler
    fun busMessage(busMessage: TaskNotRequiredMessage) {
        updateTaskState(TaskStateKey.Not_Required, busMessage)
    }

    @Handler
    fun busMessage(busMessage: TaskRequestedMessage) {
        updateTaskState(TaskStateKey.Requested, busMessage)
    }

    @Handler
    fun busMessage(busMessage: TaskStartedMessage) {
        updateTaskState(TaskStateKey.Started, busMessage)
    }

    @Handler
    fun busMessage(busMessage: TaskSuccessfulMessage) {
        val taskRecord = updateTaskState(TaskStateKey.Successful, busMessage)
        taskRecord.stdOut = busMessage.stdOut
    }

    @Handler
    fun busMessage(busMessage: TaskFailedMessage) {
        val taskRecord = updateTaskState(busMessage.result, busMessage)
        taskRecord.stdOut = busMessage.stdOut
        taskRecord.stdErr = busMessage.stdErr
        val buildRecord = getRecord(busMessage.buildRequestId)
        buildRecord.failureDescription = taskRecord.stdOut
        buildRecord.causeOfFailure = Task_Failure
    }

    override fun getRecord(buildMessage: BuildMessage): BuildRecord {
        synchronized(lock) {
            log.debug("retrieving record for {}", buildMessage)
            val existingRecord = findRecord(buildMessage.buildRequestId)
            if (existingRecord == null) {
                return createRecord(buildMessage.buildRequestId, buildMessage.delegated, buildMessage.time)
            } else {
                return existingRecord
            }
        }
    }

    override fun getRecord(uid: UUID): BuildRecord {
        synchronized(lock) {
            log.debug("retrieving record for {}", uid)
            val existingRecord = findRecord(uid) ?: throw InvalidBuildRequestIdException(uid)
            return existingRecord
        }
    }

    private fun findRecord(uid: UUID): BuildRecord? {
        var record = records[uid]
        if (record == null) {
            record = delegateBuildRecords[uid]
        }
        return record
    }

    private fun createRecord(uid: UUID, delegated: Boolean, time: OffsetDateTime): BuildRecord {
        log.debug("creating record for {}, delegate build is {}", uid, delegated)
        val record = BuildRecord(uid, time)
        if (delegated) {
            delegateBuildRecords.put(uid, record)
        } else {
            records.put(uid, record)
        }
        return record
    }

    private fun updateTaskState(newState: TaskStateKey, taskMessage: TaskMessage): TaskResult {
        log.debug("${taskMessage.javaClass.simpleName} received, build id: {}", taskMessage.buildRequestId)
        val buildRecord = getRecord(taskMessage.buildRequestId)
        val taskRecord = buildRecord.taskResult(taskMessage.taskKey)
        if (taskRecord.state == Not_Required) {
            throw InvalidBuildStateException("No further messages should be recevied when task in not required")
        }
        taskRecord.state = newState
        when (newState) {

            TaskStateKey.Cancelled -> taskRecord.completedAt = taskMessage.time
            TaskStateKey.Failed -> taskRecord.completedAt = taskMessage.time
            Not_Run -> throw InvalidBuildStateException("Cannot update the task record to be TASK NOT RUN")
            TaskStateKey.Successful -> taskRecord.completedAt = taskMessage.time
            TaskStateKey.Requested -> taskRecord.requestedAt = taskMessage.time
            Quality_Gate_Failed -> taskRecord.completedAt = taskMessage.time
            TaskStateKey.Started -> taskRecord.startedAt = taskMessage.time
        }

        if (!buildRecord.delegated) {
            hooks.publish(buildRecord)
        }
        return taskRecord
    }

    private fun updateBuildState(newState: BuildStateKey, buildMessage: BuildMessage): BuildRecord {
        log.debug("${buildMessage.javaClass.simpleName} received, build id: {}", buildMessage.buildRequestId)
        val buildRecord = getRecord(buildMessage)
        buildRecord.state = newState
        buildRecord.delegated = buildMessage.delegated

        when (newState) {
            BuildStateKey.Requested -> buildRecord.requestedAt = buildMessage.time
            BuildStateKey.Started -> buildRecord.startedAt = buildMessage.time
            BuildStateKey.Successful -> buildRecord.completedAt = buildMessage.time
            BuildStateKey.Failed -> {
                buildRecord.completedAt = buildMessage.time
            }

            BuildStateKey.Not_Started -> throw InvalidBuildStateException("Cannot update the build record to be NOT STARTED")
            BuildStateKey.Preparation_Started -> buildRecord.preparationStartedAt = buildMessage.time
            BuildStateKey.Preparation_Successful -> buildRecord.preparationCompletedAt = buildMessage.time
            BuildStateKey.Cancelled -> buildRecord.preparationCompletedAt = buildMessage.time
        }


        if (!buildMessage.delegated) {
            hooks.publish(buildRecord)
        }
        return buildRecord
    }
}


class InvalidBuildRequestIdException(buildRequestId: UUID) : RuntimeException("Invalid build request id: $buildRequestId")
class InvalidBuildStateException(msg: String) : RuntimeException(msg)