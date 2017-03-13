package uk.q3c.simplycd.agent.i18n

import uk.q3c.krail.core.persist.clazz.i18n.EnumResourceBundle

/**
 * Created by David Sowerby on 09 Jan 2017
 */
class DeveloperErrorMessages : EnumResourceBundle<DeveloperErrorMessageKey>() {

    override fun loadMap() {
        put(DeveloperErrorMessageKey.Invalid_Method, "Developer: A request was received with an Http method of '{0}'.  This URI ('{1}') only responds to '{2}'")
    }
}