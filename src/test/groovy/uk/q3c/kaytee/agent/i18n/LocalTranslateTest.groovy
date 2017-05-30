package uk.q3c.kaytee.agent.i18n

import com.google.inject.Guice
import com.google.inject.Injector
import spock.lang.Specification
import uk.q3c.kaytee.agent.TestMessageKey
import uk.q3c.kaytee.agent.eventbus.GlobalBusModule
import uk.q3c.krail.core.i18n.Translate

import static uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey.Invalid_Method

/**
 * Created by David Sowerby on 12 Mar 2017
 */

class LocalTranslateTest extends Specification {

    Injector injector
    Translate translate

    def setup() {
        injector = Guice.createInjector(new I18NModule(), new GlobalBusModule())
        translate = injector.getInstance(Translate.class)
    }

    def "default locale"() {
        expect:
        translate.from(TestMessageKey.Wiggly) == "Wiggly but cute"
        translate.from(TestMessageKey.No_Message) == "No Message"
        translate.from(Invalid_Method, "POST", "/", "GET") == "Developer: A request was received with an Http method of 'POST'.  This URI ('/') only responds to 'GET'"
    }
}
