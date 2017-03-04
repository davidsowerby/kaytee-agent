package uk.q3c.simplycd.agent.app

import ratpack.test.MainClassApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll
import uk.q3c.rest.hal.HalMapper
import uk.q3c.rest.hal.HalResource

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

    @Unroll
    "gets"() {
        when:
        def actual = aut.httpClient.get(url)

        then:
        actual.body.text == expected

        where:

        url               | expected
        "wiggly"          | "wiggly beast"
        "products/list"   | "Product List"
        "products/get"    | "Product Get"
        "products/search" | "Product Search"
        "wigglies/345"    | "returning wigglies from GET 345"
    }


    @Unroll
    "gets with Json"() {

        when:
        def actual = aut.httpClient.get("")
        HalResource halResponse = halMapper.readValue(actual.body.text, HalResource)

        then:
        halResponse.self().href == "/"

    }
}
