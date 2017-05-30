package uk.q3c.kaytee.agent.i18n

import uk.q3c.krail.core.i18n.I18NKey

/**
 * Created by David Sowerby on 21 Jan 2017
 */
interface Named {

    val nameKey: I18NKey

    /**
     * Name of this stage, translated from [nameKey]
     */
    fun name(): String
}