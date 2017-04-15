package uk.q3c.simplycd.agent.app

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.rest.hal.HalResource
import java.net.URL
import javax.annotation.concurrent.ThreadSafe

/**
 * Uses an explicit lock to manage changes to [topicMap] and [noTopicSubscribers], as the two need to be managed together
 *
 * Created by David Sowerby on 13 Mar 2017
 */
@Suppress("UNUSED_VARIABLE")
@ThreadSafe
class DefaultHooks @Inject constructor(val subscriberNotifier: SubscriberNotifier, val expandedPublicAddress: ExpandedPublicAddress) : Hooks {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    private val topicMap: MutableMap<Topic, MutableSet<Subscriber>> = mutableMapOf()
    private val lock = Any()
    private val noTopicSubscribers: MutableSet<Subscriber> = mutableSetOf()

    override fun publish(message: HalResource) {

        synchronized(lock) {
            val messageHref = expandedPublicAddress.get(message.href())
            val requestedTopic = Topic(messageHref.toURL())
            //to avoid sending duplicate messages where subscribers have been added to multiple 'levels' of a URL
            // capture the subscriber  URLs in a Set
            val notificationSet: MutableSet<Subscriber> = mutableSetOf()
            for ((k, v) in topicMap) {
                if (k.matches(requestedTopic)) {
                    notificationSet.addAll(v)
                }
            }

            subscriberNotifier.notify(notificationSet, message)
        }
    }


    private fun getTopicSubscribers(topicURL: URL): MutableSet<Subscriber> {
        val hookTopic = Topic(topicURL)
        return topicMap.getOrElse(hookTopic) { noTopicSubscribers }
    }

    override fun subscribe(topicURL: URL, subscriberCallbackUrl: URL): Boolean {
        synchronized(lock) {
            val subscribers = getTopicSubscribers(topicURL)
            if (subscribers === noTopicSubscribers) {
                return false
            } else {
                subscribers.add(Subscriber(subscriberCallbackUrl))
                return true
            }
        }
    }

    override fun registerTopic(topicURL: URL): Boolean {
        synchronized(lock) {
            val hookTopic = Topic(topicURL)
            if (topicMap.containsKey(hookTopic)) {
                log.debug("Failed to register topic: {}", hookTopic.topicUrl.toExternalForm())
                return false
            } else {
                topicMap.put(hookTopic, mutableSetOf())
                log.debug("Topic registered: {}", hookTopic.topicUrl.toExternalForm())
                return true
            }
        }
    }

    override fun subscribe(topicURL: URL, subscriberCallbackUrls: List<URL>): Boolean {
        synchronized(lock) {
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
    }

    override fun unsubscribe(topicURL: URL, subscriberCallbackUrl: URL): Boolean {
        synchronized(lock) {
            val subscribers = getTopicSubscribers(topicURL)
            return subscribers.remove(Subscriber(subscriberCallbackUrl))
        }
    }

    override fun removeTopic(topicURL: URL): Boolean {
        synchronized(lock) {
            return topicMap.remove(Topic(topicURL)) != null
        }
    }

    override fun removeSubscriber(subscriberCallbackUrl: URL) {
        synchronized(lock) {
            val callback = Subscriber(subscriberCallbackUrl)
            for ((k, v) in topicMap) {
                v.remove(callback)
            }
        }
    }


}