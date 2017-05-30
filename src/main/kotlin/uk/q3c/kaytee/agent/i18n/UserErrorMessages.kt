package uk.q3c.kaytee.agent.i18n

import uk.q3c.kaytee.agent.i18n.UserErrorMessageKey.*
import uk.q3c.krail.core.persist.clazz.i18n.EnumResourceBundle

/**
 * Created by David Sowerby on 09 Jan 2017
 */
class UserErrorMessages : EnumResourceBundle<UserErrorMessageKey>() {

    override fun loadMap() {
        put(Exception_in_Handler, "An error occurred at the server")
        put(Invalid_Method, "User: A request was received with an Http method of '{0}'.  This URI ('{1}') only responds to '{2}'")
        put(Invalid_Project_Name, "Build request received for unknown project '{0}'")
        put(Invalid_Topic, "A topic must be registered before it can be subscribed to.  '{0}' has not been registered")
        put(Unrecognised_Build_Record_Id, "There is no record for record id '{0}'")
    }
}