package uk.q3c.simplycd.i18n

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.krail.core.i18n.Translate

/**
 * Created by David Sowerby on 21 Jan 2017
 */
class DefaultNamed @Inject constructor(val translate: Translate, @Assisted override val nameKey: I18NKey) : Named {

    override fun name(): String {
        return translate.from(nameKey)
    }
}