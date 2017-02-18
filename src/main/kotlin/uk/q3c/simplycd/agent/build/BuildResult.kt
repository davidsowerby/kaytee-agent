package uk.q3c.simplycd.build

import uk.q3c.simplycd.i18n.BuildResultStateKey
import uk.q3c.simplycd.queue.QueueRequest
import java.time.LocalDateTime

/**
 * Created by David Sowerby on 13 Jan 2017
 */
data class BuildResult(val queueRequest: QueueRequest, val start: LocalDateTime, val end: LocalDateTime, val state: BuildResultStateKey) {

    fun passed(): Boolean {
        return state == BuildResultStateKey.Build_Successful
    }

    fun failed(): Boolean {
        return !passed()
    }
}