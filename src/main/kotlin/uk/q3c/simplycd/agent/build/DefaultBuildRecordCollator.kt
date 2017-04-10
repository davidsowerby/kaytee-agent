package uk.q3c.simplycd.agent.build

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.slf4j.LoggerFactory
import uk.q3c.krail.core.eventbus.GlobalBus
import uk.q3c.krail.core.eventbus.SubscribeTo
import uk.q3c.simplycd.agent.app.Hooks
import uk.q3c.simplycd.agent.i18n.BuildFailCauseKey.Build_Configuration
import uk.q3c.simplycd.agent.i18n.BuildStateKey.*
import uk.q3c.simplycd.agent.i18n.TaskResultStateKey
import uk.q3c.simplycd.agent.queue.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by David Sowerby on 25 Mar 2017
 */
@Listener @SubscribeTo(GlobalBus::class)
class DefaultBuildRecordCollator @Inject constructor(val hooks: Hooks) : BuildRecordCollator {
    override val records: MutableMap<UUID, BuildRecord> = ConcurrentHashMap()
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    @Handler
    fun busMessage(busMessage: BuildRequestedMessage) {
        log.debug("BuildRequestedMessage received, build id: {}", busMessage.buildRequestId)
        val record = BuildRecord(busMessage.buildRequestId, busMessage.time)
        records.put(busMessage.buildRequestId, record)
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: BuildStartedMessage) {
        log.debug("BuildStartedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage.buildRequestId)
        record.buildStartedAt = busMessage.time
        record.state = Build_Started
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: BuildSuccessfulMessage) {
        log.debug("BuildSuccessfulMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage.buildRequestId)
        record.buildCompletedAt = busMessage.time
        record.state = Build_Successful
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: BuildFailedMessage) {
        log.debug("BuildFailedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage.buildRequestId)
        record.state = Build_Failed
        record.causeOfFailure = BuildExceptionLookup().lookupKeyFromException(busMessage.e)
        if (record.causeOfFailure != Build_Configuration) {
            record.buildCompletedAt = busMessage.time
        }
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: PreparationStartedMessage) {
        log.debug("PreparationStartedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage.buildRequestId)
        record.preparationStartedAt = busMessage.time
        record.state = Preparation_Started
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: PreparationSuccessfulMessage) {
        log.debug("PreparationSuccessfulMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage.buildRequestId)
        record.preparationCompletedAt = busMessage.time
        record.state = Preparation_Successful
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: PreparationFailedMessage) {
        log.debug("PreparationFailedMessage received, build id: {}", busMessage.buildRequestId)
        val record = getRecord(busMessage.buildRequestId)
        record.preparationCompletedAt = busMessage.time
        record.state = Preparation_Failed
        hooks.publish(record)
    }


    @Handler
    fun busMessage(busMessage: TaskRequestedMessage) {
        log.debug("TaskRequestedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage.buildRequestId)
        record.addTask(busMessage.taskKey, busMessage.time)
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: TaskStartedMessage) {
        log.debug("TaskStartedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage.buildRequestId)
        record.updateTaskStart(busMessage.taskKey, busMessage.time)
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: TaskSuccessfulMessage) {
        log.debug("TaskSuccessfulMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage.buildRequestId)
        record.updateTaskOutcome(busMessage.taskKey, busMessage.time, TaskResultStateKey.Task_Successful)
        hooks.publish(record)
    }

    @Handler
    fun busMessage(busMessage: TaskFailedMessage) {
        log.debug("TaskFailedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val record = getRecord(busMessage.buildRequestId)
        record.updateTaskOutcome(busMessage.taskKey, busMessage.time, TaskResultStateKey.Task_Failed)
        hooks.publish(record)
    }

    override fun getRecord(buildRequestId: UUID): BuildRecord {
        val record = records.get(buildRequestId)
        if (record == null) {
            throw InvalidBuildRequestIdException(buildRequestId)
        } else {
            return record
        }
    }


}


class InvalidBuildRequestIdException(buildRequestId: UUID) : RuntimeException("Invalid build request id: $buildRequestId")
