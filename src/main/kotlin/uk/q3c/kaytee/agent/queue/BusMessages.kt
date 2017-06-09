package uk.q3c.kaytee.agent.queue

import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.plugin.TaskKey
import java.time.OffsetDateTime
import java.util.*


interface BuildMessage : BusMessage {
    val buildRequestId: UUID
}

interface TimedMessage {
    val time: OffsetDateTime
}

/**
 * Message timestamped 'now' at creation
 */
abstract class AbstractBuildMessage : BuildMessage, TimedMessage {
    override val time: OffsetDateTime = OffsetDateTime.now()
}


interface TaskMessage {
    val taskKey: TaskKey
}

data class BuildRequestMessage(val project: Project, val commitId: String) : BusMessage

data class BuildQueuedMessage(override val buildRequestId: UUID) : AbstractBuildMessage() {
    fun path(): String {
        val buildRecord = BuildRecord(buildRequestId, time)
        return buildRecord.path
    }
}

data class BuildStartedMessage(override val buildRequestId: UUID, val buildNumber: String) : AbstractBuildMessage()
data class BuildSuccessfulMessage(override val buildRequestId: UUID) : AbstractBuildMessage()
data class BuildFailedMessage(override val buildRequestId: UUID, val e: Exception) : AbstractBuildMessage()

data class PreparationStartedMessage(override val buildRequestId: UUID) : AbstractBuildMessage()
data class PreparationSuccessfulMessage(override val buildRequestId: UUID) : AbstractBuildMessage()

data class PreparationFailedMessage(override val buildRequestId: UUID, val e: Exception) : AbstractBuildMessage()

data class TaskRequestedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey) : AbstractBuildMessage(), TaskMessage
data class TaskStartedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey) : AbstractBuildMessage(), TaskMessage
data class TaskSuccessfulMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, val stdOut: String) : AbstractBuildMessage(), TaskMessage
data class TaskFailedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, val result: TaskResultStateKey, val stdErr: String, val stdOut: String) : AbstractBuildMessage(), TaskMessage




