package uk.q3c.simplycd.agent.app

import com.google.inject.Inject
import java.net.URL

/**
 *
 * Created by David Sowerby on 13 Mar 2017
 */
class BuildStatusHooks @Inject constructor(val hookNotifier: HookNotifier) : Hooks<BuildStatusMessage> {

    private val topicMap: MutableMap<HookTopic, MutableSet<HookCallback>> = mutableMapOf()
    private val allTopicHooks: MutableList<HookCallback> = mutableListOf()

    override fun publish(message: BuildStatusMessage) {
        val topic = HookTopic(URL(message.href()))

        // even if a subscriber is registered for 'all', we only want to notify registered topics
        if (topicMap.containsKey(topic)) {
            for (callback in allTopicHooks) {
                hookNotifier.notify(callback, message)
            }

            val callbackSet: Set<HookCallback>? = topicMap[topic]
            if (callbackSet != null) {
                for (callback in callbackSet) {
                    hookNotifier.notify(callback, message)
                }
            }
        }
    }

    override fun subscribeToAllTopics(subscriberCallbackUrl: URL) {
        allTopicHooks.add(HookCallback(subscriberCallbackUrl))
    }

    override fun subscribe(topicURL: URL, subscriberCallbackUrl: URL) {
        checkTopic(topicURL).add(HookCallback(subscriberCallbackUrl))
    }

    private fun checkTopic(topicURL: URL): MutableSet<HookCallback> {
        val hookTopic = HookTopic(topicURL)
        if (!topicMap.containsKey(hookTopic)) {
            topicMap.put(hookTopic, mutableSetOf())
        }
        return topicMap[hookTopic] ?: throw UnsupportedOperationException("Should be impossible, we have already checked for topic")
    }

    override fun subscribe(topicURL: URL, subscriberCallbackUrls: List<URL>) {
        val callbacks = checkTopic(topicURL)
        for (newCallbackUrl in subscriberCallbackUrls) {
            val callback = HookCallback(newCallbackUrl)
            if (!allTopicHooks.contains(callback)) {
                callbacks.add(callback)
            }
        }
    }

    override fun unsubscribe(topicURL: URL, subscriberCallbackUrl: URL) {
        val callbacks = checkTopic(topicURL)
        callbacks.remove(HookCallback(subscriberCallbackUrl))
    }

    override fun removeTopic(topicURL: URL) {
        topicMap.remove(HookTopic(topicURL))
    }

    override fun unsubscribeFromAll(subscriberCallbackUrl: URL) {
        val callback = HookCallback(subscriberCallbackUrl)
        allTopicHooks.remove(callback)
        for ((k, v) in topicMap) {
            v.remove(callback)
        }
    }


}