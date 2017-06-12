package uk.q3c.kaytee.agent.queue

import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.plugin.TaskKey
import java.time.OffsetDateTime
import java.util.*


interface BuildMessage : TimedMessage {
    val buildRequestId: UUID
    val delegated: Boolean
}

interface TimedMessage : BusMessage {
    val time: OffsetDateTime
}

/**
 * Message timestamped 'now' at creation
 */
abstract class AbstractBuildMessage : BuildMessage {
    override val time: OffsetDateTime = OffsetDateTime.now()
}

abstract class AbstractTaskMessage : TaskMessage {
    override val time: OffsetDateTime = OffsetDateTime.now()
}

interface TaskMessage : BuildMessage {
    val taskKey: TaskKey
}

interface TaskCompletedMessage : TaskMessage {
    val result: TaskStateKey
    val stdErr: String
    val stdOut: String
}

interface InitialBuildMessage : BuildMessage

data class BuildRequestMessage(val project: Project, val commitId: String) : BusMessage

data class BuildQueuedMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage(), InitialBuildMessage {
    fun path(): String {
        val buildRecord = BuildRecord(buildRequestId, time)
        return buildRecord.path
    }
}

data class BuildStartedMessage(override val buildRequestId: UUID, override val delegated: Boolean, val buildNumber: String) : AbstractBuildMessage()
data class BuildSuccessfulMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage()
data class BuildFailedMessage(override val buildRequestId: UUID, override val delegated: Boolean, val e: Exception) : AbstractBuildMessage()

data class PreparationStartedMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage(), InitialBuildMessage
data class PreparationSuccessfulMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage()

data class PreparationFailedMessage(override val buildRequestId: UUID, override val delegated: Boolean, val e: Exception) : AbstractBuildMessage()

data class TaskRequestedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean) : AbstractTaskMessage(), TaskMessage
data class TaskStartedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean) : AbstractTaskMessage(), TaskMessage
data class TaskSuccessfulMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean, val stdOut: String) : AbstractTaskMessage()
data class TaskFailedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean, val result: TaskStateKey, val stdOut: String, val stdErr: String) : AbstractTaskMessage()




