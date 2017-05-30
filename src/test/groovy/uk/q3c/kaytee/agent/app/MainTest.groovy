package uk.q3c.kaytee.agent.app

import ratpack.test.MainClassApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification
import uk.q3c.rest.hal.HalMapper

/**
 * Created by David Sowerby on 19 Feb 2017
 */
class MainTest extends Specification {

    @AutoCleanup
            aut = new MainClassApplicationUnderTest(Main.class)
    HalMapper halMapper

    def setup() {
        halMapper = new HalMapper()
    }

    // Not sure yet what we need here
    def "gets"() {
        when:
        true
        then:
        true

    }


}
