package uk.q3c.simplycd.agent.app

import com.sun.xml.internal.ws.util.StringUtils
import ratpack.http.HttpMethod
import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.impose.ForceServerListenPortImposition
import ratpack.impose.ImpositionsSpec
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification
import uk.q3c.rest.hal.HalMapper
import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.TestConfigurationException
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey

/**
 * Base class for testing Handler implementations
 *
 * Created by David Sowerby on 19 Feb 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
abstract class HandlerTest extends Specification {

    class ResponseCheck<T extends HalResource> {
        private boolean statusCheck
        private boolean selfCheck
        List<String> failMessages = new ArrayList<>()
        T result

        boolean allChecks() {
            if (!failMessages.isEmpty()) {
                println failMessages
            }
            return statusCheck && selfCheck
        }

        void statusCheck(int expected, Status actual) {
            statusCheck = Status.of(expected) == actual
            if (!statusCheck) {
                failMessages.add("status: expected '$expected' but got '$actual'")
            }
        }

        void selfCheck(String expected, String actual) {
            selfCheck = expected == actual
            if (!selfCheck) {
                failMessages.add("self: expected '$expected' but got '$actual'")
            }
        }
    }

    @AutoCleanup
    MainClassApplicationUnderTest aut
    AbstractHandler handler
    List<HttpMethod> supportedMethods
    ErrorResponseBuilder errorResponseBuilder

    // Set this to the URI stub to be interacted with, for example 'buildRequests'
    String uri

    @Delegate
    TestHttpClient client

    HalMapper halMapper

    def setup() {
        aut = createAut()
        client = aut.httpClient
        halMapper = new HalMapper()
        errorResponseBuilder = Mock(ErrorResponseBuilder)
        aut.properties
    }

    protected MainClassApplicationUnderTest createAut() {
        return new MainClassApplicationUnderTest(Main.class) {

            @Override
            protected void addImpositions(ImpositionsSpec impositions) {
                impositions.add(ForceServerListenPortImposition.of(9001))
            }
        }
    }

    def "supported methods"() {
        given:
        if (handler == null) {
            throw new TestConfigurationException("Set the handler in HandlerTest before invoking the test")
        }


        expect:
        supportedMethods == handler.validMethodCalls
    }

    protected <T extends HalResource> ResponseCheck<T> doGet(int expectedStatus, Class<T> resourceClass, String params = null, DeveloperErrorMessageKey errorKey = null) {
        return doMethod(HttpMethod.GET, expectedStatus, resourceClass, params, errorKey)
    }

    protected <T extends HalResource> ResponseCheck<T> doPost(int expectedStatus, Class<T> resourceClass, String params = null, DeveloperErrorMessageKey errorKey = null) {
        return doMethod(HttpMethod.POST, expectedStatus, resourceClass, params, errorKey)
    }


    protected <T extends HalResource> ResponseCheck<T> doMethod(HttpMethod method, int expectedStatus, Class<T> resourceClass, String params = null, DeveloperErrorMessageKey errorKey = null) {

        if (uri == null) {
            throw new TestConfigurationException("Set the 'uri' property in HandlerTest sub-class before calling this method")
        }
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
        }

        String expandedUri = url(params)
        ReceivedResponse response
        switch (method) {
            case HttpMethod.GET: response = get(expandedUri); break
            case HttpMethod.POST: response = post(expandedUri); break
            case HttpMethod.PUT: response = put(expandedUri); break
            case HttpMethod.OPTIONS: response = options(expandedUri); break
            case HttpMethod.DELETE: response = delete(expandedUri); break
            case HttpMethod.HEAD: response = head(expandedUri); break
            case HttpMethod.PATCH: response = patch(expandedUri); break
        }

        //noinspection GroovyVariableNotAssigned
        T resource = halMapper.readValue(response.body.text, resourceClass)
        ResponseCheck<T> responseCheck = new ResponseCheck()
        responseCheck.statusCheck(expectedStatus, response.status)
        if (Status.of(expectedStatus).'2xx') {
            if (params != null && params.contains("uid=")) {
                responseCheck.selfCheck("$uri/?$params", resource.self().href)
            } else {
                responseCheck.selfCheck(uri, resource.self().href)
            }
        } else {
            if (errorKey == null) {
                throw new TestConfigurationException("Must call with errorKey defined")
            }

            responseCheck.selfCheck(new SharedPublicAddress().errorDocUrl(errorKey).toString(), resource.href())
        }

        responseCheck.result = resource
        return responseCheck
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private String segmentFromErrorKey(DeveloperErrorMessageKey key) {
        return StringUtils.decapitalize(key.name().replace("_", ""))
    }

    private String url(String params) {
        if (params == null || params.isEmpty()) {
            return uri
        } else {
            return "$uri?$params"
        }

    }
}
