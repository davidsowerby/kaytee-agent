package uk.q3c.simplycd.agent.build

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.slf4j.LoggerFactory
import uk.q3c.krail.core.eventbus.GlobalBus
import uk.q3c.krail.core.eventbus.SubscribeTo
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
class DefaultBuildRecordCollator @Inject constructor() : BuildRecordCollator {
    override val results: MutableMap<UUID, BuildRecord> = ConcurrentHashMap()
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    @Handler
    fun busMessage(busMessage: BuildRequestedMessage) {
        log.debug("BuildRequestedMessage received, build id: {}", busMessage.buildRequestId)
        val result = BuildRecord(busMessage.buildRequestId, busMessage.time)
        results.put(busMessage.buildRequestId, result)
    }


    @Handler
    fun busMessage(busMessage: BuildStartedMessage) {
        log.debug("BuildStartedMessage received, build id: {}", busMessage.buildRequestId)
        val result = getResult(busMessage.buildRequestId)
        result.buildStartedAt = busMessage.time
        result.state = Build_Started
    }

    @Handler
    fun busMessage(busMessage: BuildSuccessfulMessage) {
        log.debug("BuildSuccessfulMessage received, build id: {}", busMessage.buildRequestId)
        val result = getResult(busMessage.buildRequestId)
        result.buildCompletedAt = busMessage.time
        result.state = Build_Successful
    }


    @Handler
    fun busMessage(busMessage: BuildFailedMessage) {
        log.debug("BuildFailedMessage received, build id: {}", busMessage.buildRequestId)
        val result = getResult(busMessage.buildRequestId)
        result.state = Build_Failed
        result.causeOfFailure = BuildExceptionLookup().lookupKeyFromException(busMessage.e)
        if (result.causeOfFailure != Build_Configuration) {
            result.buildCompletedAt = busMessage.time
        }
    }


    @Handler
    fun busMessage(busMessage: PreparationStartedMessage) {
        log.debug("PreparationStartedMessage received, build id: {}", busMessage.buildRequestId)
        val result = getResult(busMessage.buildRequestId)
        result.preparationStartedAt = busMessage.time
        result.state = Preparation_Started
    }

    @Handler
    fun busMessage(busMessage: PreparationSuccessfulMessage) {
        log.debug("PreparationSuccessfulMessage received, build id: {}", busMessage.buildRequestId)
        val result = getResult(busMessage.buildRequestId)
        result.preparationCompletedAt = busMessage.time
        result.state = Preparation_Successful
    }

    @Handler
    fun busMessage(busMessage: PreparationFailedMessage) {
        log.debug("PreparationFailedMessage received, build id: {}", busMessage.buildRequestId)
        val result = getResult(busMessage.buildRequestId)
        result.preparationCompletedAt = busMessage.time
        result.state = Preparation_Failed
    }


    @Handler
    fun busMessage(busMessage: TaskRequestedMessage) {
        log.debug("TaskRequestedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val result = getResult(busMessage.buildRequestId)
        result.addTask(busMessage.taskKey, busMessage.time)
    }

    @Handler
    fun busMessage(busMessage: TaskStartedMessage) {
        log.debug("TaskStartedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val result = getResult(busMessage.buildRequestId)
        result.updateTaskStart(busMessage.taskKey, busMessage.time)
    }

    @Handler
    fun busMessage(busMessage: TaskSuccessfulMessage) {
        log.debug("TaskSuccessfulMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val result = getResult(busMessage.buildRequestId)
        result.updateTaskOutcome(busMessage.taskKey, busMessage.time, TaskResultStateKey.Task_Successful)
    }

    @Handler
    fun busMessage(busMessage: TaskFailedMessage) {
        log.debug("TaskFailedMessage received, build id: {}, task: {}", busMessage.buildRequestId, busMessage.taskKey)
        val result = getResult(busMessage.buildRequestId)
        result.updateTaskOutcome(busMessage.taskKey, busMessage.time, TaskResultStateKey.Task_Failed)
    }

    override fun getResult(buildRequestId: UUID): BuildRecord {
        val result = results.get(buildRequestId)
        if (result == null) {
            throw InvalidBuildRequestIdException(buildRequestId)
        } else {
            return result
        }
    }


}


class InvalidBuildRequestIdException(buildRequestId: UUID) : RuntimeException("Invalid build request id: $buildRequestId")
