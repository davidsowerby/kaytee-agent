package uk.q3c.simplycd.agent.i18n

import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.simplycd.i18n.Named

/**
 *
 * Assisted inject factory for [Named]
 *
 * Created by David Sowerby on 21 Jan 2017
 */
interface NamedFactory {

    fun create(nameKey: I18NKey): Named
}