package uk.q3c.kaytee.agent.i18n

import net.engio.mbassy.bus.IMessagePublication
import net.engio.mbassy.bus.MBassador
import spock.lang.Specification
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.krail.core.i18n.CurrentLocale
/**
 * Created by David Sowerby on 10 Apr 2017
 */
class SpecifiedCurrentLocaleTest extends Specification {

    CurrentLocale currentLocale
    GlobalBusProvider globalBusProvider = Mock(GlobalBusProvider)
    MBassador globalBus = Mock(MBassador)
    IMessagePublication messsagePublication = Mock()

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
        given:
        def msg
        IMessagePublication imp = Mock(IMessagePublication)

        when:
        currentLocale.setLocale(Locale.CANADA_FRENCH, true)

        then:

        1 * globalBusProvider.get() >> globalBus

        1 * globalBus.publishAsync(_) >> { arguments ->
            msg = arguments[0]
            messsagePublication
        }
        msg.changeSource == currentLocale
        msg.newLocale == Locale.CANADA_FRENCH
    }

    def "setLocale defaults to fireListeners=true"() {
        given:
        LocaleChangeBusMessage msg

        when:
        currentLocale.setLocale(Locale.CANADA_FRENCH)

        then:
        1 * globalBusProvider.get() >> globalBus
        1 * globalBus.publishAsync(_) >> { arguments ->
            msg = arguments[0] as LocaleChangeBusMessage
            messsagePublication
        }
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
