package uk.q3c.simplycd.agent

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.jackson.Jackson
import ratpack.test.embed.EmbeddedApp
import spock.lang.Unroll
import uk.q3c.rest.hal.HalMapper
import uk.q3c.simplycd.agent.api.BuildRequest
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.app.SubscriptionRequest
import uk.q3c.simplycd.agent.build.BuildRecord
import uk.q3c.simplycd.agent.build.TaskResult
import uk.q3c.simplycd.agent.i18n.TaskKey

import java.time.LocalDateTime

import static uk.q3c.simplycd.agent.i18n.BuildFailCauseKey.Not_Applicable
import static uk.q3c.simplycd.agent.i18n.BuildFailCauseKey.Task_Failure
import static uk.q3c.simplycd.agent.i18n.BuildStateKey.Build_Failed
import static uk.q3c.simplycd.agent.i18n.BuildStateKey.Build_Successful
import static uk.q3c.simplycd.agent.i18n.TaskResultStateKey.Task_Failed
import static uk.q3c.simplycd.agent.i18n.TaskResultStateKey.Task_Successful

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


    @Unroll
    def "run build #testDesc"() {
        given:
        defaultSubscribe()
        final String fullProjectName = "davidsowerby/simplycd-test"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, commitId)
        LocalDateTime timeoutAt = LocalDateTime.now().plusSeconds(timeout)

        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(ConstantsKt.buildRequests)
        while (LocalDateTime.now().isBefore(timeoutAt) && subscriberMessages.size() < expectedMessages) {
            println "Waiting for build to complete"
            Thread.sleep(1000)
        }


        then:
        subscriberMessages.size() == expectedMessages
        BuildRecord finalRecord = subscriberMessages.get(expectedMessages - 1)
        finalRecord.state == finalBuildState
        finalRecord.causeOfFailure == causeOfFailure
        TaskResult taskResult = finalRecord.taskResults.get(TaskKey.Unit_Test)
        taskResult.outcome == unitTestResult
        taskResult.completedAt.isBefore(finalRecord.buildCompletedAt) || taskResult.completedAt.isEqual(finalRecord.buildCompletedAt)


        where:
        commitId                                   | testDesc                    | timeout | expectedMessages | finalBuildState  | causeOfFailure | unitTestResult
        "7c3a779e17d65ec255b4c7d40b14950ea6ce232e" | "successful unit test only" | 20      | 8                | Build_Successful | Not_Applicable | Task_Successful
        "a118dc6598ae3f2b65ae2c4042a54e1418e0f3b9" | "unit test failure"         | 20      | 8                | Build_Failed     | Task_Failure   | Task_Failed
    }


    private void defaultSubscribe() {
        subscribe("http://localhost:9001/buildRecords", subscriberUri)
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
