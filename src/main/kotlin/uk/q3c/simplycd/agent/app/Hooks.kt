package uk.q3c.simplycd.agent.app

import uk.q3c.rest.hal.HalResource
import java.net.URL

/**
 *  Maintains web hooks for notifying other services of changes - usually changes of state, or updates of progress, from the build progress, but could be for anything
 *
 * Created by David Sowerby on 13 Mar 2017
 */
interface Hooks<in M : HalResource> {

    fun publish(message: M)

    /**
     * Subscribe to all topics
     */
    fun subscribeToAllTopics(subscriberCallbackUrl: URL)

    /**
     * Subscribe to a topic
     */
    fun subscribe(topicURL: URL, subscriberCallbackUrl: URL)

    /**
     * Subscribe a number of subscribers to the same topic
     */
    fun subscribe(topicURL: URL, subscriberCallbackUrls: List<URL>)

    /**
     * Unsubscribe a specific subscriber
     */
    fun unsubscribe(topicURL: URL, subscriberCallbackUrl: URL)

    /**
     * Remove a topic
     */
    fun removeTopic(topicURL: URL)

    /**
     * Remove a subscriber from all topics
     */
    fun unsubscribeFromAll(subscriberCallbackUrl: URL)
}

interface HookNotifier {
    fun notify(callback: HookCallback, message: HalResource)
}

data class HookTopic(val topicUrl: URL)

data class HookCallback(val callbackUrl: URL)