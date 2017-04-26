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

//import static uk.q3c.simplycd.agent.i18n.BuildFailCauseKey.*
//import static uk.q3c.simplycd.agent.i18n.BuildStateKey.*
//import static uk.q3c.simplycd.agent.i18n.TaskResultStateKey.*
import static uk.q3c.simplycd.agent.i18n.BuildStateKey.Failed
import static uk.q3c.simplycd.agent.i18n.BuildStateKey.Successful
import static uk.q3c.simplycd.agent.i18n.TaskResultStateKey.*

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
//TODO response    WARN  r.s.internal.NettyHandlerAdapter - No response sent for PUT request to / (last handler: closure at line 40 of FunctionalTest1.groovy)
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
        Thread.sleep(1000) // sometimes catches additional messages when expectedMessages is set too low

        then:
        subscriberMessages.size() == expectedMessages
        BuildRecord finalRecord = subscriberMessages.get(expectedMessages - 1)
        finalRecord.state == finalBuildState
        finalRecord.causeOfFailure == causeOfFailure
        TaskResult unitTestActual = finalRecord.taskResults.get(TaskKey.Unit_Test)
        unitTestActual.outcome == unitTestExpected
        unitTestActual.completedAt.isBefore(finalRecord.buildCompletedAt) || unitTestActual.completedAt.isEqual(finalRecord.buildCompletedAt)
        unitTestActual.stdOut.contains(unitStdOut)
        unitTestActual.stdErr.contains(unitStdErr)

        TaskResult integrationTestActual = finalRecord.taskResults.get(TaskKey.Integration_Test)
        integrationTestActual.outcome == integrationTestExpected
        integrationTestActual.completedAt.isBefore(finalRecord.buildCompletedAt) || integrationTestActual.completedAt.isEqual(finalRecord.buildCompletedAt)


        where:
        commitId                                   | testDesc                    | timeout | expectedMessages | finalBuildState | causeOfFailure | unitTestExpected    | integrationTestExpected | unitStdOut             | unitStdErr
        "7c3a779e17d65ec255b4c7d40b14950ea6ce232e" | "successful unit test only" | 20      | 8                | Successful      | Not_Applicable | Task_Successful     | Task_Not_Run            | "BUILD SUCCESSFUL"     | ""
        "4b55add3e402758ce4e589d8e4bffb79d1d3dda2" | "unit test failure"         | 20      | 8                | Failed          | Task_Failure   | Task_Failed         | Task_Not_Run            | "BUILD FAILED"         | "failing tests"
        "561f9cbf658ca8e08a4b56917e145c3cb467579f" | "unit test pass, QG fail"   | 20      | 8                | Failed          | Task_Failure   | Quality_Gate_Failed | Task_Not_Run            | "Code Coverage Failed" | "Code coverage failed"
        "26baa588bd3a9b26928b840ddae87e40548a9005" | "integration test passes"   | 20      | 11               | Successful      | Not_Applicable | Task_Successful     | Task_Successful         | "BUILD SUCCESSFUL"     | ""
        "ebfea9da5835df16ab23931de18c5e728094028c" | "integration test fails"    | 20      | 11               | Failed          | Task_Failure   | Task_Successful     | Task_Failed             | "BUILD SUCCESSFUL"     | ""
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
