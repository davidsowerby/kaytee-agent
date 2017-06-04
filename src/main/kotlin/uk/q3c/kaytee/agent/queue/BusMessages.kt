package uk.q3c.kaytee.agent.queue

import uk.q3c.build.gitplus.notSpecified
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import java.time.OffsetDateTime
import java.util.*


interface BuildMessage : BusMessage {
    val buildRequestId: UUID
}

/**
 * Message timestamped 'now' at creation
 */
abstract class TimedMessage : BuildMessage {
    val time: OffsetDateTime = OffsetDateTime.now()
}


interface TaskMessage {
    val taskKey: TaskKey
}

data class BuildRequestMessage(override val buildRequestId: UUID, var repoUser: String = notSpecified) : TimedMessage()

data class BuildRequestedMessage(override val buildRequestId: UUID) : TimedMessage() {
    fun path(): String {
        val buildRecord = BuildRecord(buildRequestId, time)
        return buildRecord.path
    }
}

data class BuildStartedMessage(override val buildRequestId: UUID, val buildNumber: String) : TimedMessage()
data class BuildSuccessfulMessage(override val buildRequestId: UUID) : TimedMessage()
data class BuildFailedMessage(override val buildRequestId: UUID, val e: Exception) : TimedMessage()

data class PreparationStartedMessage(override val buildRequestId: UUID) : TimedMessage()
data class PreparationSuccessfulMessage(override val buildRequestId: UUID) : TimedMessage()

data class PreparationFailedMessage(override val buildRequestId: UUID, val e: Exception) : TimedMessage()

data class TaskRequestedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey) : TimedMessage(), TaskMessage
data class TaskStartedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey) : TimedMessage(), TaskMessage
data class TaskSuccessfulMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, val stdOut: String) : TimedMessage(), TaskMessage
data class TaskFailedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, val result: TaskResultStateKey, val stdErr: String, val stdOut: String) : TimedMessage(), TaskMessage




