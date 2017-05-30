package uk.q3c.kaytee.agent.app

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.queue.BuildRequestedMessage

/**
 * Created by David Sowerby on 20 Mar 2017
 */
@Listener
class DefaultTopicRegistrar @Inject constructor(val hooks: Hooks, val expandedPublicAddress: ExpandedPublicAddress) : TopicRegistrar {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    init {
        hooks.registerTopic(expandedPublicAddress.get(buildRecords).toURL())
    }

    @Handler
    override fun buildRequest(message: BuildRequestedMessage) {
        log.debug("Registering build request {}, as Hooks topic", message.buildRequestId)
        val expandedAddress = expandedPublicAddress.get(message.path())
        hooks.registerTopic(expandedAddress.toURL())
    }
}