package uk.q3c.simplycd.agent.app

import groovy.json.JsonOutput
import org.apache.http.HttpStatus
import ratpack.guice.BindingsImposition
import ratpack.http.client.RequestSpec
import ratpack.impose.ImpositionsSpec
import ratpack.test.MainClassApplicationUnderTest

/**
 * Created by David Sowerby on 13 Mar 2017
 */
class SubscriptionHandlerTest extends HandlerTest {

    Hooks mockHooks = Mock(Hooks)
    URL topicURL = new URL("https://example.com/topic/1")
    URL callbackURL = new URL("https://callback.com/listener")


    def setup() {
        uri = ConstantsKt.subscriptions
    }

    @Override
    protected MainClassApplicationUnderTest createAut() {
        return new MainClassApplicationUnderTest(Main.class) {
            @Override
            protected void addImpositions(ImpositionsSpec impositions) {
                impositions.add(
                        BindingsImposition.of {
                            it.bindInstance(Hooks.class, mockHooks)
                        })
            }
        }
    }

    def "post hook successfully"() {
        given:
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(topicURL, callbackURL)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(subscriptionRequest))
        }
        post(uri)

        then:
        1 * mockHooks.subscribe(topicURL, callbackURL) >> true
        response.statusCode == HttpStatus.SC_OK
        response.getBody().getText() == "OK"
    }

    def "post hook fails"() {
        given:
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(topicURL, callbackURL)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(subscriptionRequest))
        }
        post(uri)

        then:
        1 * mockHooks.subscribe(topicURL, callbackURL) >> false
        response.statusCode == HttpStatus.SC_BAD_REQUEST
        ErrorResponse errorResponse = halMapper.readValue(response.body.text, ErrorResponse)
        with(errorResponse) {
            httpCode == HttpStatus.SC_BAD_REQUEST
            detailCode == "Invalid_Topic"
        }
    }

    def "unsubscribe"() {
        given:
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(topicURL, callbackURL)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(subscriptionRequest))
        }
        delete(uri)

        then:
        1 * mockHooks.unsubscribe(topicURL, callbackURL) >> true
        response.statusCode == HttpStatus.SC_OK
        response.body.text == "OK"
    }

    def "unsubscribe when not subscribed"() {
        given:
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(topicURL, callbackURL)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(subscriptionRequest))
        }
        delete(uri)

        then: "does not fail but gives more info"
        1 * mockHooks.unsubscribe(topicURL, callbackURL) >> false
        response.statusCode == HttpStatus.SC_OK
        response.body.text == "OK Subscription did not exist"
    }
}
