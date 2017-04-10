package uk.q3c.simplycd.agent.i18n

import uk.q3c.krail.core.persist.clazz.i18n.EnumResourceBundle
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey.*

/**
 * Created by David Sowerby on 09 Jan 2017
 */
class DeveloperErrorMessages : EnumResourceBundle<DeveloperErrorMessageKey>() {

    override fun loadMap() {
        put(Exception_in_Handler, "Exception occurred in Handler, exception message: {0}")
        put(Invalid_Project_Name, "Build request received for unknown project '{0}'")
        put(Invalid_Method, "Developer: A request was received with an Http method of '{0}'.  This URI ('{1}') only responds to '{2}'")
        put(Invalid_Topic, "A topic must be registered before it can be subscribed to.  '{0}' has not been registered")
        put(Unrecognised_Build_Record_Id, "The Build Record Id of '{0}' is not recognised ")
    }
}