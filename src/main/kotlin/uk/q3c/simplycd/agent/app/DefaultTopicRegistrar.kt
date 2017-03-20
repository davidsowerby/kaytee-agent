package uk.q3c.simplycd.agent.app

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.queue.BuildRequestedMessage
import java.net.URL

/**
 * Created by David Sowerby on 20 Mar 2017
 */
@Listener
class DefaultTopicRegistrar @Inject constructor(val hooks: Hooks) : TopicRegistrar {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    @Handler
    override fun buildRequest(message: BuildRequestedMessage) {
        log.debug("Registering build request {}, as Hooks topic", message.buildRequest.identity())
        hooks.registerTopic(URL(href("$buildRequests/${message.buildRequest.uid}")))
    }
}