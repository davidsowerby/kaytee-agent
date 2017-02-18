package uk.q3c.simplycd.queue

import uk.q3c.build.gitplus.notSpecified
import uk.q3c.krail.core.eventbus.BusMessage
import uk.q3c.simplycd.i18n.BuildResultStateKey
import uk.q3c.simplycd.project.Project
import java.time.LocalDateTime

/**
 * Created by David Sowerby on 26 Jan 2017
 */

/**
 * this has to have a no-args constructor for RestEasy Jackson
 */
data class BuildRequestMessage(var repoUser: String = notSpecified) : BusMessage

data class BuildRequestedMessage(val buildRequest: BuildRequest) : BusMessage
data class BuildStartedMessage(val buildRequest: BuildRequest) : BusMessage
data class BuildCompletedMessage(val project: Project, val buildNumber: Int, val buildRequest: BuildRequest) : BusMessage

data class PreparationStartedMessage(val buildRequest: BuildRequest) : BusMessage
data class PreparationCompletedMessage(val buildRequest: BuildRequest) : BusMessage

data class TaskRequestedMessage(val taskRequest: TaskRequest) : BusMessage
data class TaskStartedMessage(val taskRequest: TaskRequest, val start: LocalDateTime) : BusMessage
data class TaskCompletedMessage(val taskRequest: TaskRequest, val start: LocalDateTime, val end: LocalDateTime, val result: BuildResultStateKey) : BusMessage




