package uk.q3c.kaytee.agent.eventbus

import com.google.inject.Guice
import com.google.inject.Injector
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import spock.lang.Specification
import uk.q3c.util.testutil.LogMonitor

/**
 * Created by David Sowerby on 16 Jul 2017
 */
class GlobalBusModuleTest extends Specification {

    @Listener
    static class GlobalBusConsumer {

        GlobalBusConsumer() {
        }

        @Handler
        void msg(BusMessage message) {
            throw new IOException()
        }

    }

    static class TestMessage implements BusMessage {

    }

    Injector injector

    def setup() {
        injector = Guice.createInjector(new GlobalBusModule())
    }

    def "Exception during publication is logged"() {
        given:
        LogMonitor logMonitor = new LogMonitor()
        logMonitor.addClassFilter(DefaultEventBusErrorHandler)
        GlobalBusConsumer consumer = injector.getInstance(GlobalBusConsumer)
        GlobalBusProvider busProvider = injector.getInstance(GlobalBusProvider)

        when:
        busProvider.get().publish(new TestMessage())


        then:
        logMonitor.errorLogs().size() == 1
        logMonitor.errorLogs().get(0).contains("Error during message publication")
        logMonitor.errorLogs().get(0).contains("publishedMessage=")
    }
}
