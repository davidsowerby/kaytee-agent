package uk.q3c.simplycd.agent.i18n

import net.engio.mbassy.bus.common.PubSubSupport
import spock.lang.Specification
import uk.q3c.krail.core.i18n.CurrentLocale
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider

/**
 * Created by David Sowerby on 10 Apr 2017
 */
class SpecifiedCurrentLocaleTest extends Specification {

    CurrentLocale currentLocale
    GlobalBusProvider globalBusProvider = Mock(GlobalBusProvider)
    PubSubSupport globalBus = Mock(PubSubSupport)

    def setup() {
        currentLocale = new SpecifiedCurrentLocale(globalBusProvider)
    }

    def "read from environment not supported"() {

        when:
        currentLocale.readFromEnvironment()

        then:
        thrown UnsupportedOperationException
    }

    def "setLocale with true, sends bus message"() {
        LocaleChangeBusMessage msg

        when:
        currentLocale.setLocale(Locale.CANADA_FRENCH, true)

        then:
        1 * globalBusProvider.get() >> globalBus
        1 * globalBus.publish(_) >> { arguments -> msg = arguments[0] as LocaleChangeBusMessage }
        msg.changeSource == currentLocale
        msg.newLocale == Locale.CANADA_FRENCH
    }

    def "setLocale defaults to fireListeners=true"() {
        LocaleChangeBusMessage msg

        when:
        currentLocale.setLocale(Locale.CANADA_FRENCH)

        then:
        1 * globalBusProvider.get() >> globalBus
        1 * globalBus.publish(_) >> { arguments -> msg = arguments[0] as LocaleChangeBusMessage }
        msg.changeSource == currentLocale
        msg.newLocale == Locale.CANADA_FRENCH
    }

    def "setLocale fireListeners=false"() {
        LocaleChangeBusMessage msg

        when:
        currentLocale.setLocale(Locale.CANADA_FRENCH, false)

        then: "no bus message sent"
        0 * globalBusProvider.get() >> globalBus
    }
}
