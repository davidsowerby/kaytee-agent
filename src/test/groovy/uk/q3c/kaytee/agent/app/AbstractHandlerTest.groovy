package uk.q3c.kaytee.agent.app

import com.google.common.collect.ImmutableList
import org.jetbrains.annotations.NotNull
import ratpack.handling.Context
import ratpack.http.Request
import ratpack.http.Response
import spock.lang.Specification
import uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey

import static ratpack.http.HttpMethod.*

/**
 * Created by David Sowerby on 20 Mar 2017
 */
class AbstractHandlerTest extends Specification {
    ErrorResponseBuilder errorResponseBuilder = Mock(ErrorResponseBuilder)
    Context context = Mock(Context)
    Request request = Mock(Request)
    Response response = Mock(Response)

    class TestHandler extends AbstractHandler {


        TestHandler(
                @NotNull ErrorResponseBuilder errorResponseBuilder) {
            super(errorResponseBuilder)
            uri = "test"
        }
    }

    class TestHandler2 extends TestHandler {

        TestHandler2(@NotNull ErrorResponseBuilder errorResponseBuilder) {
            super(errorResponseBuilder)
        }

        void get(Context context) {
            throw new UnsupportedOperationException("testing")
        }
    }


    TestHandler handler

    def setup() {
        handler = new TestHandler(errorResponseBuilder)
        handler.validMethodCalls = ImmutableList.of()
        context.request >> request
        context.response >> response

    }

    def "Method calls returns error response when not defined"() {
        given:
        request.method >> method
        request.uri >> "/tests"

        when:
        handler.handle(context)

        then:
        1 * errorResponseBuilder.invalidMethod(request.uri, method, ImmutableList.of())

        where:
        method  | blank
        GET     | 1
        POST    | 1
        PUT     | 1
        OPTIONS | 1
        PATCH   | 1
    }

    def "exception thrown when method included in validMethodCalls but not defined"() {
        given:
        handler.validMethodCalls = ImmutableList.of(method)
        request.method >> method
        request.uri >> "/tests"

        when:
        handler.handle(context)

        then:
        1 * errorResponseBuilder.build('test', DeveloperErrorMessageKey.Exception_in_Handler, "Either override this method to respond or modify 'validMethodCalls' to exclude $method")

        where:
        method  | blank
        GET     | 1
        POST    | 1
        PUT     | 1
        OPTIONS | 1
        PATCH   | 1
    }

    def "exception thrown by handler code returns error response with 500 status"() {
        given:
        handler = new TestHandler2(errorResponseBuilder)
        handler.validMethodCalls = ImmutableList.of(GET)
        request.method >> GET
        request.uri >> "/tests"

        when:
        handler.handle(context)

        then:
        1 * errorResponseBuilder.build('test', DeveloperErrorMessageKey.Exception_in_Handler, ['testing'])
        1 * response.status(500)

    }
}
