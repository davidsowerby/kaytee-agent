package uk.q3c.simplycd.agent.i18n

import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.krail.core.i18n.Translate
import java.text.Collator
import java.util.*

/**
 * Created by David Sowerby on 07 Mar 2017
 */
class LocalTranslate : Translate {
    override fun from(checkLocaleIsSupported: Boolean, key: I18NKey?, locale: Locale?, vararg arguments: Any?): String {
        TODO()
    }

    override fun from(key: I18NKey?, locale: Locale?, vararg arguments: Any?): String {
        TODO()
    }

    override fun from(key: I18NKey?, vararg arguments: Any?): String {
        TODO()
    }

    override fun collator(): Collator {
        TODO()
    }
}