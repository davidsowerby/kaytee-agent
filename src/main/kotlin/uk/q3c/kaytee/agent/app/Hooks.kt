package uk.q3c.kaytee.agent.app

import com.google.inject.Inject
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import uk.q3c.rest.hal.HalMapper
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
    fun notify(subscribers: MutableSet<Subscriber>, message: HalResource)
}

class DefaultSubscriberNotifier @Inject constructor(val halMapper: HalMapper) : SubscriberNotifier {
    private val log = LoggerFactory.getLogger(this.javaClass.name)


    override fun notify(subscribers: MutableSet<Subscriber>, message: HalResource) {
        // Using Apache HttpClient here.  The Ratpack client is designed to work within the Thread management of Ratpack
        // but we have broken out of that by using the Global Bus

        val httpClient: CloseableHttpClient = HttpClients.createDefault()
        try {
            for (subscriber in subscribers) {
                log.debug("Notifying subscriber {} of change to {}", subscriber.callbackUrl, message.href())
                val httpPut = HttpPut(subscriber.callbackUrl.toExternalForm())
                httpPut.addHeader("accept", "application/hal+json")
                val jsonString = halMapper.writeValueAsString(message)
                val entity: StringEntity = StringEntity(jsonString, ContentType.APPLICATION_JSON)
                httpPut.entity = entity
                val response = httpClient.execute(httpPut)
                try {
                    log.debug("Response from {} was '{}'", subscriber.callbackUrl, response.statusLine)
                } finally {
                    response.close()
                }
            }
        } catch (e: Exception) {
            log.error("Unable to publish message", e)
        } finally {
            httpClient.close()
        }
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