package uk.q3c.simplycd.agent.app

import com.google.common.collect.ImmutableList
import org.apache.http.HttpStatus
import ratpack.http.HttpMethod
import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey

/**
 * Created by David Sowerby on 19 Feb 2017
 */
class RootHandlerTest extends HandlerTest {

    def setup() {
        handler = new RootHandler(errorResponseBuilder)
        supportedMethods = ImmutableList.of(HttpMethod.GET)
        uri = "/"
    }

    def "gets with Json"() {

        when:
        ResponseCheck responseCheck = doGet(HttpStatus.SC_OK, HalResource)
        HalResource halResponse = responseCheck.getResult()

        then:
        responseCheck.allChecks()
        halResponse.hasLink(ConstantsKt.buildRequests)
        halResponse.link(ConstantsKt.buildRequests).href == ConstantsKt.buildRequests
    }

    def "post fails with invalid method"() {

        when:
        ResponseCheck responseCheck = doPost(HttpStatus.SC_METHOD_NOT_ALLOWED, ErrorResponse, null, DeveloperErrorMessageKey.Invalid_Method)
        ErrorResponse halResponse = responseCheck.getResult()

        then:
        halResponse.self().href == new SharedPublicAddress().errorDocUrl(DeveloperErrorMessageKey.Invalid_Method).toString()
        halResponse.developerMessage == "Developer: A request was received with an Http method of 'POST'.  This URI ('/') only responds to '[GET]'"
        halResponse.userMessage == "User: A request was received with an Http method of 'POST'.  This URI ('/') only responds to '[GET]'"
        halResponse.detailCode == "Invalid_Method"
        halResponse.httpCode == 405
    }
}
