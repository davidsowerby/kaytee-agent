package uk.q3c.kaytee.agent.app

import uk.q3c.kaytee.agent.queue.BuildQueuedMessage

/**
 * Registers / de-registers topic from [Hooks]
 *
 * Created by David Sowerby on 20 Mar 2017
 */
interface TopicRegistrar {

    fun buildRequest(message: BuildQueuedMessage)
}