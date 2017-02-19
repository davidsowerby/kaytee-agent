package uk.q3c.simplycd.agent.app

import ratpack.test.MainClassApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Created by David Sowerby on 19 Feb 2017
 */
class MainTest extends Specification {

    @AutoCleanup
    def aut = new MainClassApplicationUnderTest(Main.class)

    def setup() {

    }

    def "doit"() {
        when:
        def response = aut.httpClient.get()

        then:
        response.body.text == "GET"
    }
}
