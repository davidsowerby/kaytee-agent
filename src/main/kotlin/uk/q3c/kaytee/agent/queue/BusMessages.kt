package uk.q3c.kaytee.agent.queue

import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.plugin.TaskKey
import java.time.OffsetDateTime
import java.util.*


interface BuildMessage : TimedMessage {
    val buildRequestId: UUID
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
    val result: TaskResultStateKey
    val stdErr: String
    val stdOut: String
}

interface InitialBuildMessage : BuildMessage {
    val delegateBuild: Boolean
}

data class BuildRequestMessage(val project: Project, val commitId: String) : BusMessage

data class BuildQueuedMessage(override val buildRequestId: UUID, override val delegateBuild: Boolean) : AbstractBuildMessage(), InitialBuildMessage {
    fun path(): String {
        val buildRecord = BuildRecord(buildRequestId, time)
        return buildRecord.path
    }
}

data class BuildStartedMessage(override val buildRequestId: UUID, val buildNumber: String) : AbstractBuildMessage()
data class BuildSuccessfulMessage(override val buildRequestId: UUID) : AbstractBuildMessage()
data class BuildFailedMessage(override val buildRequestId: UUID, val e: Exception) : AbstractBuildMessage()

data class PreparationStartedMessage(override val buildRequestId: UUID, override val delegateBuild: Boolean) : AbstractBuildMessage(), InitialBuildMessage
data class PreparationSuccessfulMessage(override val buildRequestId: UUID) : AbstractBuildMessage()

data class PreparationFailedMessage(override val buildRequestId: UUID, val e: Exception) : AbstractBuildMessage()

data class TaskRequestedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey) : AbstractTaskMessage(), TaskMessage
data class TaskStartedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey) : AbstractTaskMessage(), TaskMessage
data class TaskSuccessfulMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val stdOut: String, override val result: TaskResultStateKey = TaskResultStateKey.Task_Successful, override val stdErr: String = "") : AbstractTaskMessage(), TaskCompletedMessage
data class TaskFailedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val result: TaskResultStateKey, override val stdOut: String, override val stdErr: String) : AbstractTaskMessage(), TaskCompletedMessage




