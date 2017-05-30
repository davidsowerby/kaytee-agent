package uk.q3c.kaytee.agent.queue

import uk.q3c.build.gitplus.notSpecified
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by David Sowerby on 26 Jan 2017
 */
/**
 * Message timestamped 'now' at creation
 */
abstract class TimedMessage : BusMessage {
    val time: OffsetDateTime = OffsetDateTime.now()
}

data class BuildRequestMessage(val buildRequestId: UUID, var repoUser: String = notSpecified) : TimedMessage()

data class BuildRequestedMessage(val buildRequestId: UUID) : TimedMessage() {
    fun path(): String {
        val buildRecord = BuildRecord(buildRequestId, time)
        return buildRecord.path
    }
}

data class BuildStartedMessage(val buildRequestId: UUID, val buildNumber: String) : TimedMessage()
data class BuildSuccessfulMessage(val buildRequestId: UUID) : TimedMessage()
data class BuildFailedMessage(val buildRequestId: UUID, val e: Exception) : TimedMessage()

data class PreparationStartedMessage(val buildRequestId: UUID) : TimedMessage()
data class PreparationSuccessfulMessage(val buildRequestId: UUID) : TimedMessage()

data class PreparationFailedMessage(val buildRequestId: UUID, val e: Exception) : TimedMessage()

data class TaskRequestedMessage(val buildRequestId: UUID, val taskKey: TaskKey) : TimedMessage()
data class TaskStartedMessage(val buildRequestId: UUID, val taskKey: TaskKey) : TimedMessage()
data class TaskSuccessfulMessage(val buildRequestId: UUID, val taskKey: TaskKey, val stdOut: String) : TimedMessage()
data class TaskFailedMessage(val buildRequestId: UUID, val taskKey: TaskKey, val result: TaskResultStateKey, val stdErr: String, val stdOut: String) : TimedMessage()




