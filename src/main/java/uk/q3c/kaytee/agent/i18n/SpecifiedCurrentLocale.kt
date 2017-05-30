package uk.q3c.kaytee.agent.i18n

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.krail.core.i18n.CurrentLocale
import java.util.*

/**
 * A [CurrentLocale] implementation where the locale to use is explicitly set - perhaps by REST call or similar
 *
 * Useful in a microservices architecture where there is no value in directly supporting user choices / browser settings
 *
 * Created by David Sowerby on 07 Mar 2017
 */
class SpecifiedCurrentLocale @Inject constructor(val globalBusProvider: GlobalBusProvider) : CurrentLocale {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    var currentLocale: Locale = Locale.UK

    override fun getLocale(): Locale {
        return currentLocale
    }

    override fun readFromEnvironment() {
        throw UnsupportedOperationException("Locale must be explicitly set in this implementation")
    }

    override fun setLocale(locale: Locale) {
        setLocale(locale, true)
    }

    override fun setLocale(locale: Locale, fireListeners: Boolean) {
        if (locale != this.currentLocale) {
            currentLocale = locale
            log.debug("CurrentLocale set to {}", locale)
            if (fireListeners) {
                log.debug("publish locale change")
                globalBusProvider.get().publish(LocaleChangeBusMessage(this, locale))
            }
        }
    }
}