package uk.q3c.simplycd.agent.app

import spock.lang.Unroll
import uk.q3c.rest.hal.HalResource

/**
 * Created by David Sowerby on 19 Feb 2017
 */
class RootHandlerTest extends HandlerTest {

    @Unroll
    "gets with Json"() {

        when:
        def actual = aut.httpClient.get("")
        HalResource halResponse = halMapper.readValue(actual.body.text, HalResource)

        then:
        halResponse.self().href == "/"
        halResponse.link(ConstantsKt.buildRequests).href == ConstantsKt.buildRequests
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
