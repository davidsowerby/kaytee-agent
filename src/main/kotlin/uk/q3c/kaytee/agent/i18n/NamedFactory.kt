package uk.q3c.kaytee.agent.i18n

import uk.q3c.krail.core.i18n.I18NKey

/**
 *
 * Assisted inject factory for [Named]
 *
 * Created by David Sowerby on 21 Jan 2017
 */
interface NamedFactory {

    fun create(nameKey: I18NKey): Named
}