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
import uk.q3c.simplycd.agent.api.BuildRequest
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.app.SubscriptionRequest
import uk.q3c.simplycd.agent.build.BuildRecord
import uk.q3c.simplycd.agent.build.TaskResult
import uk.q3c.simplycd.agent.i18n.BuildFailCauseKey
import uk.q3c.simplycd.agent.i18n.BuildStateKey
import uk.q3c.simplycd.agent.i18n.TaskKey
import uk.q3c.simplycd.agent.i18n.TaskResultStateKey

import java.time.LocalDateTime

/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class FunctionalTest1 extends FunctionalTestBase {

    static List<BuildRecord> subscriberMessages

    EmbeddedApp subscriber = GroovyEmbeddedApp.ratpack {
        bindings {
            add(ObjectMapper.class, new HalMapper())
        }
        handlers {
            all {
                context.parse(Jackson.fromJson(BuildRecord.class)).then { buildRecord ->
                    subscriberMessages.add(buildRecord)
                }

            }
        }
    }

    String subscriberUri
//    HttpClient httpClient

    def setup() {
        subscriberMessages = new ArrayList<>()
        subscriberUri = subscriber.address.toString()
    }

    def "subscribe to build records"() {
        when:
        subscribe("http://localhost:9001/buildRecords", subscriberUri)

        then:
        response.status == Status.OK
    }


    def "run a simple known good build"() {
        given:
        subscribe("http://localhost:9001/buildRecords", subscriberUri)
        final String fullProjectName = "davidsowerby/simplycd-test"
        final String commitId = "7c3a779e17d65ec255b4c7d40b14950ea6ce232e"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, commitId)
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20)

        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(ConstantsKt.buildRequests)
        while (LocalDateTime.now().isBefore(timeout) && subscriberMessages.size() < 8) {
            println "Waiting for build to complete"
            Thread.sleep(1000)
        }


        then:
        subscriberMessages.size() == 8
        BuildRecord finalRecord = subscriberMessages.get(7)
        finalRecord.state == BuildStateKey.Build_Successful
        finalRecord.causeOfFailure == BuildFailCauseKey.Not_Applicable
        TaskResult taskResult = finalRecord.taskResults.get(TaskKey.Unit_Test)
        taskResult.outcome == TaskResultStateKey.Task_Successful
        taskResult.completedAt.isBefore(finalRecord.buildCompletedAt) || taskResult.completedAt.isEqual(finalRecord.buildCompletedAt)
    }

    def "run a simple build, with failure"() {
        given:
        subscribe("http://localhost:9001/buildRecords", subscriberUri)
        final String fullProjectName = "davidsowerby/simplycd-test"
        final String commitId = "a118dc6598ae3f2b65ae2c4042a54e1418e0f3b9"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, commitId)
        LocalDateTime timeout = LocalDateTime.now().plusSeconds(20)

        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(ConstantsKt.buildRequests)
        while (LocalDateTime.now().isBefore(timeout) && subscriberMessages.size() < 8) {
            println "Waiting for build to complete"
            Thread.sleep(1000)
        }


        then:
        subscriberMessages.size() == 8
        BuildRecord finalRecord = subscriberMessages.get(7)
        finalRecord.state == BuildStateKey.Build_Failed
        finalRecord.causeOfFailure == BuildFailCauseKey.Task_Failure
        TaskResult taskResult = finalRecord.taskResults.get(TaskKey.Unit_Test)
        taskResult.outcome == TaskResultStateKey.Task_Failed
        taskResult.completedAt.isBefore(finalRecord.buildCompletedAt) || taskResult.completedAt.isEqual(finalRecord.buildCompletedAt)
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
