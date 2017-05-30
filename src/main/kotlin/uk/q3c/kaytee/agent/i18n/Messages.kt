package uk.q3c.kaytee.i18n

import uk.q3c.kaytee.agent.i18n.MessageKey
import uk.q3c.kaytee.agent.i18n.MessageKey.*
import uk.q3c.krail.core.persist.clazz.i18n.EnumResourceBundle

/**
 * Created by David Sowerby on 09 Jan 2017
 */
class Messages : EnumResourceBundle<MessageKey>() {

    override fun loadMap() {
        put(Build_Request_Removed_from_Queue, "The Build QueueRequest for '{0}', with commit id {1}, has been removed from the queue ")
        put(Build_Request_Stop_Not_Found, "Unable to stop build request {0}, it is not an active build")
        put(Build_Request_Stop_Sent, "The build for '{0}', with commit id {1}, is being stopped")

    }
}