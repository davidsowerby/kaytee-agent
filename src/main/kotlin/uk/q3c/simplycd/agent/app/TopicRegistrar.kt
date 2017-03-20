package uk.q3c.simplycd.agent.app

import uk.q3c.simplycd.agent.queue.BuildRequestedMessage

/**
 * Registers / de-registers topic from [Hooks]
 *
 * Created by David Sowerby on 20 Mar 2017
 */
interface TopicRegistrar {

    fun buildRequest(message: BuildRequestedMessage)
}