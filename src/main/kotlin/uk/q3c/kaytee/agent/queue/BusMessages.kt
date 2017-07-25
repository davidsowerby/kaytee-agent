package uk.q3c.kaytee.agent.queue

import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.plugin.TaskKey
import java.time.OffsetDateTime
import java.util.*


interface BuildMessage : TimedMessage {
    val buildRequestId: UUID
    val delegated: Boolean
    val targetState: BuildStateKey
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

interface TaskMessage : TimedMessage {
    val buildRequestId: UUID
    val delegated: Boolean
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
    override val targetState = BuildStateKey.Requested
    fun path(): String {
        val buildRecord = BuildRecord(buildRequestId, time, delegated)
        return buildRecord.path
    }
}

data class BuildStartedMessage(override val buildRequestId: UUID, override val delegated: Boolean, val buildNumber: String) : AbstractBuildMessage() {
    override val targetState = BuildStateKey.Started
}

data class BuildSuccessfulMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage() {
    override val targetState = BuildStateKey.Successful
}

data class BuildFailedMessage(override val buildRequestId: UUID, override val delegated: Boolean, val e: Exception) : AbstractBuildMessage() {
    override val targetState = BuildStateKey.Failed
}
/**
 * The build itself is completed when either a [BuildSuccessfulMessage] or a [BuildFailedMessage] is sent, but there may be some post processing to do.
 * This message is sent to indicate that all of that post-processing is also complete
 */
data class BuildProcessCompletedMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage() {
    override val targetState = BuildStateKey.Complete
}

data class PreparationStartedMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage(), InitialBuildMessage {
    override val targetState = BuildStateKey.Preparation_Started
}

data class PreparationSuccessfulMessage(override val buildRequestId: UUID, override val delegated: Boolean) : AbstractBuildMessage() {
    override val targetState = BuildStateKey.Preparation_Successful
}

data class PreparationFailedMessage(override val buildRequestId: UUID, override val delegated: Boolean, val e: Exception) : AbstractBuildMessage() {
    override val targetState = BuildStateKey.Preparation_Failed
}

data class TaskNotRequiredMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean) : AbstractTaskMessage(), TaskMessage
data class TaskRequestedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean) : AbstractTaskMessage(), TaskMessage
data class TaskStartedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean) : AbstractTaskMessage(), TaskMessage
data class TaskSuccessfulMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean, val stdOut: String) : AbstractTaskMessage()
data class TaskFailedMessage(override val buildRequestId: UUID, override val taskKey: TaskKey, override val delegated: Boolean, val result: TaskStateKey, val stdOut: String, val stdErr: String, val exception: Exception) : AbstractTaskMessage()

data class BuildMessageEnvelope(val buildMessage: BuildMessage) : BusMessage






