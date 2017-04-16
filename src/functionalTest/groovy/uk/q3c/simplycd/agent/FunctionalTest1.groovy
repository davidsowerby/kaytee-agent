package uk.q3c.simplycd.agent

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.jackson.Jackson
import ratpack.test.embed.EmbeddedApp
import uk.q3c.rest.hal.HalMapper
import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.api.BuildRequest
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.app.SubscriptionRequest

import java.time.LocalDateTime

/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class FunctionalTest1 extends FunctionalTestBase {

    static List<String> subscriberMessages = new ArrayList<>()

    EmbeddedApp subscriber = GroovyEmbeddedApp.ratpack {
        bindings {
            add(ObjectMapper.class, new HalMapper())
        }
        handlers {
            all {
                context.parse(Jackson.fromJson(HalResource.class)).then { halResource ->
                    String state = halResource.members().get("state")
                    subscriberMessages.add(state)
                }

            }
        }
    }

    String subscriberUri
//    HttpClient httpClient

    def setup() {
        subscriberUri = subscriber.address.toString()
    }

    def "subscribe to build records"() {
        when:
        subscribe("http://localhost:9001/buildRecords", subscriberUri)

        then:
        response.status == Status.OK
    }


    def "run a known good build"() {
        given:
        subscribe("http://localhost:9001/buildRecords", subscriberUri)
        final String fullProjectName = "davidsowerby/simplycd-test"
        final String commitId = "7c3a779e17d65ec255b4c7d40b14950ea6ce232e"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, commitId)
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(30)

        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(ConstantsKt.buildRequests)
        while (LocalDateTime.now().isBefore(timeout) && !subscriberMessages.contains("Build_Successful")) {
            println "Waiting for build to complete"
            Thread.sleep(1000)
        }


        then:
        subscriberMessages.containsAll("Preparation_Started", "Preparation_Successful", "Build_Started", "Build_Successful")
    }

    private ReceivedResponse subscribe(String toTopic, String subscriberCallback) {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(new URL(toTopic), new URL(subscriberCallback))
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(subscriptionRequest))
        }
        ReceivedResponse response = post(ConstantsKt.subscriptions)
        return response

    }
}
