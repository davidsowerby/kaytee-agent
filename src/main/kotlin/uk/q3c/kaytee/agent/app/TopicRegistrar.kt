package uk.q3c.kaytee.agent.app

import uk.q3c.kaytee.agent.queue.BuildRequestedMessage

/**
 * Registers / de-registers topic from [Hooks]
 *
 * Created by David Sowerby on 20 Mar 2017
 */
interface TopicRegistrar {

    fun buildRequest(message: BuildRequestedMessage)
}