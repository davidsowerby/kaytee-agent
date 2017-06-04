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
    private val lock = Any()
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    @Handler
    fun busMessage(busMessage: BuildRequestedMessage) {
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
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: TaskRequestedMessage) {
        log.debug("TaskRequestedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage)
        record.addTask(busMessage.taskKey, busMessage.time)
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: TaskStartedMessage) {
        log.debug("TaskStartedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage)
        record.updateTaskStart(busMessage.taskKey, busMessage.time)
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: TaskSuccessfulMessage) {
        log.debug("TaskSuccessfulMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage)
        record.updateTaskOutcome(busMessage.taskKey, busMessage.time, TaskResultStateKey.Task_Successful, busMessage.stdOut)
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: TaskFailedMessage) {
        log.debug("TaskFailedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage)
        record.updateTaskOutcome(busMessage.taskKey, busMessage.time, busMessage.result, busMessage.stdOut, busMessage.stdErr)
        hooks.publish(record)
    }

    /**
     * In theory, the [BuildRequestedMessage] should arrive before the [PreparationStartedMessage] for a given build, but in practice
     * they are so close together that the order they are received may be reversed by the time they have been transported by
     * the event bus.
     *
     * For that reason, a record is constructed and added to [records] for either message, if one does not exist already
     *
     *
     */

    override fun getRecord(buildMessage: TimedMessage): BuildRecord {
        synchronized(lock) {
            var record = records[buildMessage.buildRequestId]
            if (record == null) {
                if (buildMessage is BuildRequestedMessage || buildMessage is PreparationStartedMessage) {
                    log.debug("creating build record for {}", buildMessage.buildRequestId)
                    record = BuildRecord(buildMessage.buildRequestId, buildMessage.time)
                    records.put(buildMessage.buildRequestId, record)
                } else {
                    throw InvalidBuildRequestIdException(buildMessage.buildRequestId)
                }
            }
            return record
        }
    }

}


class InvalidBuildRequestIdException(buildRequestId: UUID) : RuntimeException("Invalid build request id: $buildRequestId")
