package uk.q3c.simplycd.agent.app

import com.google.inject.Inject
import uk.q3c.rest.hal.HalResource
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * Created by David Sowerby on 13 Mar 2017
 */
class DefaultHooks @Inject constructor(val subscriberNotifier: SubscriberNotifier) : Hooks {

    private val topicMap: MutableMap<Topic, MutableSet<Subscriber>> = ConcurrentHashMap()
    private val lock = Any()
    private val noTopicSubscribers: MutableSet<Subscriber> = mutableSetOf()

    override fun publish(message: HalResource) {
        val requestedTopic = Topic(URL(message.href()))
        //to avoid sending duplicate messages where subscribers have been added to multiple 'levels' of a URL
        // capture the subscriber  URLs in a Set
        val notificationSet: MutableSet<Subscriber> = mutableSetOf()
        for ((k, v) in topicMap) {
            if (k.matches(requestedTopic)) {
                notificationSet.addAll(v)
            }
        }
        for (subscriber in notificationSet) {
            subscriberNotifier.notify(subscriber, message)
        }
    }


    private fun getTopicSubscribers(topicURL: URL): MutableSet<Subscriber> {
        val hookTopic = Topic(topicURL)
        return topicMap.getOrElse(hookTopic) { noTopicSubscribers }
    }

    override fun subscribe(topicURL: URL, subscriberCallbackUrl: URL): Boolean {
        val subscribers = getTopicSubscribers(topicURL)
        if (subscribers === noTopicSubscribers) {
            return false
        } else {
            subscribers.add(Subscriber(subscriberCallbackUrl))
            return true
        }
    }

    override fun registerTopic(topicURL: URL): Boolean {
        val hookTopic = Topic(topicURL)
        if (topicMap.containsKey(hookTopic)) {
            return false
        } else {
            topicMap.put(hookTopic, mutableSetOf())
            return true
        }
    }

    override fun subscribe(topicURL: URL, subscriberCallbackUrls: List<URL>): Boolean {
        val subscribers = getTopicSubscribers(topicURL)
        if (subscribers === noTopicSubscribers) {
            return false
        } else {
            for (newCallbackUrl in subscriberCallbackUrls) {
                val callback = Subscriber(newCallbackUrl)
                subscribers.add(callback)
            }
            return true
        }
    }

    override fun unsubscribe(topicURL: URL, subscriberCallbackUrl: URL) {
        val subscribers = getTopicSubscribers(topicURL)
        subscribers.remove(Subscriber(subscriberCallbackUrl))
    }

    override fun removeTopic(topicURL: URL): Boolean {
        return topicMap.remove(Topic(topicURL)) != null
    }

    override fun removeSubscriber(subscriberCallbackUrl: URL) {
        val callback = Subscriber(subscriberCallbackUrl)
        for ((k, v) in topicMap) {
            v.remove(callback)
        }
    }


}