package uk.q3c.simplycd.agent.app

import ratpack.test.MainClassApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll
import uk.q3c.rest.hal.HalMapper
import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.api.ErrorResponse

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

    @Unroll
    "posts with Json"() {

        when:
        def actual = aut.httpClient.post(url)
        ErrorResponse halResponse = halMapper.readValue(actual.body.text, ErrorResponse)

        then:
        halResponse.self().href == url
        halResponse.developerMessage == "Developer: A request was received with an Http method of 'POST'.  This URI ('/') only responds to 'GET'"
        halResponse.userMessage == "User: A request was received with an Http method of 'POST'.  This URI ('/') only responds to 'GET'"
        halResponse.detailCode == "InvalidMethod"
        halResponse.httpCode == 405

        where:

        url | href
        ""  | "/"
    }
}
