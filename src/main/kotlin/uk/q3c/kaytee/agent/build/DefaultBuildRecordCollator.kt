package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.app.Hooks
import uk.q3c.kaytee.agent.eventbus.GlobalBus
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.eventbus.SubscribeTo
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Task_Failure
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.BuildStateKey.*
import uk.q3c.kaytee.agent.i18n.BuildStateKey.Cancelled
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.i18n.TaskStateKey.*
import uk.q3c.kaytee.agent.i18n.TaskStateKey.Failed
import uk.q3c.kaytee.agent.i18n.TaskStateKey.Successful
import uk.q3c.kaytee.agent.queue.*

import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe

/**
 * Because all the messages handled here are despatched asynchronously, most likely from different threads, there is no
 * guarantee that they will arrive in the order that might be expected.
 *
 * In theory, a real build will not generate messages close together, as each step will take more than a trivial amount of time.
 *
 * However, to ensure resilience, no assumption is made about the order in which messages are received.
 *
 * For that reason, a record is created whenever a message is received with a build id not already held by the collator.
 *
 * To retrieve a build record externally, use [getRecord] with a UUID parameter - this will not create a record but
 * throw an exception if the record does not exist
 *
 * Created by David Sowerby on 25 Mar 2017
 */
@ThreadSafe
@Listener @SubscribeTo(GlobalBus::class)
class DefaultBuildRecordCollator @Inject constructor(val hooks: Hooks, val globalBusProvider: GlobalBusProvider, val stateModel: StateModel) : BuildRecordCollator {


    override val records: MutableMap<UUID, BuildRecord> = ConcurrentHashMap()
    val delegateBuildRecords: MutableMap<UUID, BuildRecord> = ConcurrentHashMap()
    private val lock = Any()
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override val buildStateCount: MutableMap<BuildStateKey, Int> = HashMap()


    override fun updateBuildStateCount() {
        synchronized(lock) {
            buildStateCount.clear()
            for (key in BuildStateKey.values()) {
                buildStateCount.put(key, 0)
            }
            for (record in records.values) {
                val newValue = buildStateCount[record.state]!!.inc()
                buildStateCount.put(record.state, newValue)
            }
        }
    }


    @Handler
    fun buildMessage(busMessage: BuildMessage) {
        synchronized(lock) {
            updateBuildState(busMessage)
        }
    }


    @Handler
    fun busMessage(busMessage: TaskNotRequiredMessage) {
        synchronized(lock) {
            updateTaskState(TaskStateKey.Not_Required, busMessage)
        }
    }

    @Handler
    fun busMessage(busMessage: TaskRequestedMessage) {
        synchronized(lock) {
            updateTaskState(TaskStateKey.Requested, busMessage)
        }
    }

    @Handler
    fun busMessage(busMessage: TaskStartedMessage) {
        synchronized(lock) {
            updateTaskState(TaskStateKey.Started, busMessage)
        }
    }

    @Handler
    fun busMessage(busMessage: TaskSuccessfulMessage) {
        synchronized(lock) {
            val taskRecord = updateTaskState(Successful, busMessage)
            taskRecord.stdOut = busMessage.stdOut
        }
    }


    @Handler
    fun busMessage(busMessage: BuildMessageEnvelope) {
        log.debug("Build ${busMessage.buildMessage.buildRequestId} received BuildMessageEnvelope, containing ${busMessage.buildMessage.javaClass.simpleName}")
        buildMessage(busMessage.buildMessage)
    }

    @Handler
    fun busMessage(busMessage: TaskFailedMessage) {
        synchronized(lock) {
            log.debug("Received {} for {}", busMessage.javaClass.simpleName, busMessage.taskKey.name)
            val taskRecord = updateTaskState(busMessage.result, busMessage)
            taskRecord.stdOut = busMessage.stdOut
            taskRecord.stdErr = busMessage.stdErr
            val buildRecord = getRecord(busMessage.buildRequestId)
            buildRecord.failureDescription = taskRecord.stdOut
            log.debug("Build record failure description set to: ${buildRecord.failureDescription}")
            buildRecord.causeOfFailure = Task_Failure
            buildRecord.failedTask = busMessage.taskKey
        }
    }

    override fun getOrCreateRecord(buildMessage: BuildMessage): BuildRecord {
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
        val record = BuildRecord(uid, time, delegated)
        if (delegated) {
            delegateBuildRecords.put(uid, record)
        } else {
            records.put(uid, record)
        }
        return record
    }

    private fun updateTaskState(newState: TaskStateKey, taskMessage: TaskMessage): TaskResult {
        log.debug("${taskMessage.javaClass.simpleName} received, task {}, build id: {}", taskMessage.taskKey, taskMessage.buildRequestId)
        val buildRecord = getRecord(taskMessage.buildRequestId)
        val taskRecord = buildRecord.taskResult(taskMessage.taskKey)
        if (taskRecord.state == Not_Required) {
            throw InvalidBuildStateException("No further messages should be received when task in not required")
        }
        taskRecord.state = newState
        when (newState) {

            TaskStateKey.Cancelled -> taskRecord.completedAt = taskMessage.time
            Failed -> taskRecord.completedAt = taskMessage.time
            Not_Run -> throw InvalidBuildStateException("Cannot update the task record to be TASK NOT RUN")
            Successful -> taskRecord.completedAt = taskMessage.time
            TaskStateKey.Requested -> taskRecord.requestedAt = taskMessage.time
            Quality_Gate_Failed -> taskRecord.completedAt = taskMessage.time
            TaskStateKey.Started -> taskRecord.startedAt = taskMessage.time
        }

        if (!buildRecord.delegated) {
            hooks.publish(buildRecord)
        }
        return taskRecord
    }

    override fun hasRecord(uid: UUID): Boolean {
        synchronized(lock) {
            return findRecord(uid) != null
        }
    }

    private fun updateBuildState(buildMessage: BuildMessage): BuildRecord {
        log.debug("Build {} ${buildMessage.javaClass.simpleName} received", buildMessage.buildRequestId)
        val buildRecord = getOrCreateRecord(buildMessage)
        val newState: BuildStateKey = buildMessage.targetState

        if (stateModel.currentStateValid(buildRecord.state, newState)) {
            log.debug("Build ${buildMessage.buildRequestId} state change is valid, current state is '${buildRecord.state}', target state is '$newState'")
            processNewState(buildMessage, newState, buildRecord, buildMessage.time)
        } else {
            log.debug("Build ${buildMessage.buildRequestId} state change is NOT valid, current state is '${buildRecord.state}', target state is '$newState'")
            resendMessage(buildMessage)
        }

        if (!buildMessage.delegated) {
            hooks.publish(buildRecord)
        }
        return buildRecord
    }

    private fun processNewState(buildMessage: BuildMessage, newState: BuildStateKey, buildRecord: BuildRecord, time: OffsetDateTime) {
        when (newState) {
            Not_Started -> throw InvalidBuildStateException("Cannot update the build record to be NOT STARTED")
            BuildStateKey.Requested -> buildRecord.requestedAt = time
            BuildStateKey.Started -> {
                buildRecord.startedAt = time
            }
            BuildStateKey.Successful -> {
                buildRecord.completedAt = time
                buildRecord.outcome = BuildStateKey.Successful
            }
            BuildStateKey.Failed -> {
                buildRecord.completedAt = time
                buildRecord.outcome = BuildStateKey.Failed
                val busMessage = buildMessage as BuildFailedMessage
                buildRecord.causeOfFailure = BuildExceptionLookup().lookupKeyFromException(busMessage.e)
                buildRecord.failureDescription =
                        if (busMessage.e.message != null) {
                            busMessage.e.message as String
                        } else {
                            busMessage.e.javaClass.simpleName
                        }
            }


            Preparation_Started -> {
                buildRecord.preparationStartedAt = time
            }
            Preparation_Successful -> {
                buildRecord.preparationCompletedAt = time
            }
            Cancelled -> buildRecord.preparationCompletedAt = time

            Preparation_Failed -> {
                buildRecord.outcome = BuildStateKey.Preparation_Failed
                buildRecord.preparationCompletedAt = time
                buildRecord.causeOfFailure = BuildFailCauseKey.Preparation_Failure
                val busMessage = buildMessage as PreparationFailedMessage
                val stacktrace = ExceptionUtils.getRootCauseStackTrace(busMessage.e)
                buildRecord.failureDescription = stacktrace.joinToString(separator = "\n")
            }

            Complete -> {
                buildRecord.processCompletedAt = time
            }
        }
        val oldState = buildRecord.state
        buildRecord.state = buildMessage.targetState
        log.debug("Build ${buildRecord.uid} changed state from ${oldState} to ${buildMessage.targetState}")
    }


    /**
     * If messages arrive in the wrong order, we re-send the message in an envelope (this prevents other message consumers being confused)
     * This should give things time to catch up
     *
     * TODO some form of time out - maybe only resend a limited number of times before giving up, recorded in BuildMessage
     */
    private fun resendMessage(buildMessage: BuildMessage) {
        globalBusProvider.get().publishAsync(BuildMessageEnvelope(buildMessage))
    }
}


class InvalidBuildRequestIdException(buildRequestId: UUID) : RuntimeException("Invalid build request id: $buildRequestId")
class InvalidBuildStateException(msg: String) : RuntimeException(msg)