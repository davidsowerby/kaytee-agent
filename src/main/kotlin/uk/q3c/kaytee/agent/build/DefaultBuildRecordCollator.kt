package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.app.Hooks
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Build_Configuration
import uk.q3c.kaytee.agent.i18n.BuildStateKey.*
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.krail.core.eventbus.GlobalBus
import uk.q3c.krail.core.eventbus.SubscribeTo
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
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
        log.debug("BuildRequestedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage)
        record.requestedAt = busMessage.time

        // We don't want to change state if PreparationStarted has already been received
        if (record.state == Not_Started) {
            record.state = Requested
        }
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: BuildStartedMessage) {
        log.debug("BuildStartedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage)
        record.buildStartedAt = busMessage.time
        record.state = Build_Started
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: BuildSuccessfulMessage) {
        log.debug("BuildSuccessfulMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage)
        record.buildCompletedAt = busMessage.time
        record.state = Successful
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: BuildFailedMessage) {
        log.debug("BuildFailedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage)
        record.state = Failed
        record.causeOfFailure = BuildExceptionLookup().lookupKeyFromException(busMessage.e)
        if (record.causeOfFailure != Build_Configuration) {
            record.buildCompletedAt = busMessage.time
        }
        record.failureDescription =
                if (busMessage.e.message != null) {
                    busMessage.e.message as String
                } else {
                    busMessage.e.javaClass.simpleName
                }
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: PreparationStartedMessage) {
        log.debug("PreparationStartedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage)
        record.preparationStartedAt = busMessage.time
        record.state = Preparation_Started
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: PreparationSuccessfulMessage) {
        log.debug("PreparationSuccessfulMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage)
        record.preparationCompletedAt = busMessage.time
        record.state = Preparation_Successful
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: PreparationFailedMessage) {
        log.debug("PreparationFailedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage)
        record.preparationCompletedAt = busMessage.time
        record.state = Preparation_Failed
        record.failureDescription =
                if (busMessage.e.message != null) {
                    busMessage.e.message as String
                } else {
                    busMessage.e.javaClass.simpleName
                }
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: TaskRequestedMessage) {
        updateTask(busMessage, TaskResultStateKey.Task_Requested)
    }

    @Handler
    fun busMessage(busMessage: TaskStartedMessage) {
        updateTask(busMessage, TaskResultStateKey.Task_Started)
    }

    @Handler
    fun busMessage(busMessage: TaskSuccessfulMessage) {
        updateTask(busMessage, TaskResultStateKey.Task_Successful)
    }

    @Handler
    fun busMessage(busMessage: TaskFailedMessage) {
        updateTask(busMessage, busMessage.result)
    }

    fun updateTask(buildMessage: TaskMessage, outcome: TaskResultStateKey) {
        val messageType = buildMessage.javaClass.simpleName
        val buildRequestId = buildMessage.buildRequestId
        val taskKey = buildMessage.taskKey
        var stdOut = ""
        var stdErr = ""

        if (buildMessage is TaskCompletedMessage) {
            stdOut = buildMessage.stdOut
            stdErr = buildMessage.stdErr
        }

        log.debug("{$messageType} received, build id: {}, task: {}", buildRequestId, taskKey)
        val record = getRecord(buildMessage)

        when (buildMessage) {
            is TaskRequestedMessage -> record.updateTaskRequested(taskKey, buildMessage.time)
            is TaskStartedMessage -> record.updateTaskStart(taskKey, buildMessage.time)
            is TaskCompletedMessage -> record.updateTaskOutcome(taskKey, buildMessage.time, outcome, stdOut, stdErr)
        }

        log.debug("after task update, build id: {} state is: ${record.summary()}", buildRequestId)
        hooks.publish(record)
    }

    /**
     * In theory, the [BuildQueuedMessage] should arrive before the [PreparationStartedMessage] for a given build, but in practice
     * it seemed that the order they are received may be reversed by the time they have been transported by the event bus.
     *
     * For that reason, a record is constructed and added to [records] for either message, if one does not exist already
     *
     * (Note: This diagnosis is a little in doubt, as there were some questions about the way the block was synchronized
     * - now resolved - but the fix code has been left in place as it does no harm)
     *
     */

    override fun getRecord(buildMessage: BuildMessage): BuildRecord {

        synchronized(lock) {
            log.debug("retrieving record for {}", buildMessage.buildRequestId)
            var record = records[buildMessage.buildRequestId]
            if (record == null) {
                record = delegateBuildRecords[buildMessage.buildRequestId]
            }
            if (record == null) {
                if (buildMessage is InitialBuildMessage) {
                    log.debug("creating build record for {}", buildMessage.buildRequestId)
                    record = BuildRecord(buildMessage.buildRequestId, buildMessage.time)

                    if (buildMessage.delegateBuild) {
                        delegateBuildRecords.put(buildMessage.buildRequestId, record)
                    } else {
                        records.put(buildMessage.buildRequestId, record)
                    }
                } else {
                    throw InvalidBuildRequestIdException(buildMessage.buildRequestId)
                }
            }
            return record
        }
    }


}


class InvalidBuildRequestIdException(buildRequestId: UUID) : RuntimeException("Invalid build request id: $buildRequestId")
