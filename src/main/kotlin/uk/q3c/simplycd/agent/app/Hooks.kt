package uk.q3c.simplycd.agent.app

import uk.q3c.rest.hal.HalResource
import java.net.URL

/**
 *  Maintains web hooks for notifying other services of changes.  Works on a hierarchical URL principle, so that a subscription to
 *  "http:example.com/blog/posts" will receive notification of changes to all posts, while a subscription to
 *  "http:example.com/blog/posts/1" will receive notification of changes to that one specific post
 *
 *  Subscribers will receive only one notification for each call to [publish], even if they have subscribed to multiple levels of the topic
 *  hierarchy
 *
 * Created by David Sowerby on 13 Mar 2017
 */
interface Hooks {

    /**
     * Publish an update message to all subscribers of a topic.  The topic is identified by the *self* property of the message.
     *
     *  Subscribers will receive only one notification per call to this method, even if they have subscribed to multiple levels of the topic
     *  hierarchy
     */
    fun publish(message: HalResource)

    /**
     * Subscribe to a topic.
     *
     * @return false if the topic has not been registered, true if the subscription was successful
     */
    fun subscribe(topicURL: URL, subscriberCallbackUrl: URL): Boolean

    /**
     * Subscribe a number of subscribers to the same topic
     *
     * @return false if the topic has not been registered, true if the subscription was successful
     */
    fun subscribe(topicURL: URL, subscriberCallbackUrls: List<URL>): Boolean

    /**
     * Unsubscribe a subscriber from a topic
     */
    fun unsubscribe(topicURL: URL, subscriberCallbackUrl: URL): Boolean

    /**
     * Remove a topic
     *
     * @return true if topic removed, false if topic was not there to remove
     */
    fun removeTopic(topicURL: URL): Boolean

    /**
     * Remove a subscriber from all topics
     */
    fun removeSubscriber(subscriberCallbackUrl: URL)

    /**
     * Registers [topicURL] as a topic for subscribers to subscriber to.  Note that it is up to the caller to register exactly what is required.
     * For example, given a topic of "http:example.com/blog/posts", you may want to allow subscription to all post, or just individual posts, or both
     *
     * To enable subscription to all posts, register topic as "http:example.com/blog/posts"
     * To enable subscription to individual posts, register each topic as "http:example.com/blog/posts/1", "http:example.com/blog/posts/2" etc
     *
     *
     * @return true if topic added, false if the topic had already been registered
     */
    fun registerTopic(topicURL: URL): Boolean
}

interface SubscriberNotifier {
    fun notify(callback: Subscriber, message: HalResource)
}

class DefaultSubscriberNotifier : SubscriberNotifier {
    override fun notify(callback: Subscriber, message: HalResource) {
        TODO()
    }

}

data class Topic(val topicUrl: URL) {
    // does not get used in equals / hash
    private val externalForm: String

    init {
        val s = topicUrl.toExternalForm()
        if (s.endsWith("/")) {
            externalForm = s.substring(0, s.length - 1)
        } else {
            externalForm = s
        }
    }

    fun matches(candidate: Topic): Boolean {
        return candidate.externalForm.startsWith(externalForm)
    }
}

data class Subscriber(val callbackUrl: URL)