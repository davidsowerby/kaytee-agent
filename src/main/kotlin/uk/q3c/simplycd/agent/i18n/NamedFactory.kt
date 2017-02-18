package uk.q3c.simplycd.i18n

import uk.q3c.krail.core.i18n.I18NKey

/**
 * Created by David Sowerby on 21 Jan 2017
 */
interface NamedFactory {

    fun create(nameKey: I18NKey): Named
}