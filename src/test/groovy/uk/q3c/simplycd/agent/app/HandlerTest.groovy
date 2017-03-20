package uk.q3c.simplycd.agent.app

import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification
import uk.q3c.rest.hal.HalMapper

/**
 * Base class for testing Handler implementations
 *
 * Created by David Sowerby on 19 Feb 2017
 */
abstract class HandlerTest extends Specification {

    @AutoCleanup
    MainClassApplicationUnderTest aut

    // Set this to the URI stub to be interacted with, for example 'buildRequests'
    String uri

    @Delegate
    TestHttpClient client

    HalMapper halMapper

    def setup() {
        aut = createAut()
        client = aut.httpClient
        halMapper = new HalMapper()
        aut.properties
    }

    protected MainClassApplicationUnderTest createAut() {
        return new MainClassApplicationUnderTest(Main.class)
    }

}
