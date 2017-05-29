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
import static uk.q3c.simplycd.agent.i18n.BuildStateKey.Successful
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
        final String fullProjectName = "davidsowerby/kaytee-test"
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
            timeoutAt = LocalDateTime.now().plusSeconds(timeout)
            Thread.sleep(1000)
        }
        Thread.sleep(2000) // sometimes catches additional messages when expectedMessages is set too low

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
        integrationTestActual.stdOut.contains(iTestStdOut)
        integrationTestActual.stdErr.contains(iTestStdErr)

        where:
        commitId                                   | testDesc                    | timeout | expectedMessages | finalBuildState | causeOfFailure | unitTestExpected | integrationTestExpected | unitStdOut         | unitStdErr | iTestStdOut        | iTestStdErr
        "1c1f4a41b8d59d325f0ca6938ee6846c6dd6a0f5" | "full cycle, simple tests " | 20      | 29               | Successful      | Not_Applicable | Task_Successful  | Task_Successful         | "BUILD SUCCESSFUL" | ""         | "BUILD SUCCESSFUL" | ""
//        "922a7d39d68ab5a95b879a92477a7d0e0dd3f76d" | "unit test failure"              | 20      | 8                | Failed          | Task_Failure   | Task_Failed         | Task_Not_Run            | "BUILD FAILED"         | "failing tests"        | ""                 | ""
//        "ddedcf08ee724fb059da8365143eeb23c3b58b44" | "unit test pass, QG fail"        | 20      | 8                | Failed          | Task_Failure   | Quality_Gate_Failed | Task_Not_Run            | "Code Coverage Failed" | "Code coverage failed" | ""                 | ""
//        "7dce1f433e9d8bf42fbb5bba2f88ea540c239628" | "integration test passes"        | 20      | 11               | Successful      | Not_Applicable | Task_Successful     | Task_Successful         | "BUILD SUCCESSFUL"     | ""                     | "BUILD SUCCESSFUL" | ""
//        "03f90f0cfb4476c62bfc67b9798f4612e74703c7" | "integration test fails"         | 20      | 11               | Failed          | Task_Failure   | Task_Successful     | Task_Failed             | "BUILD SUCCESSFUL"     | ""                     | "BUILD FAILED"     | "failing tests"
//        "4a60a14dae0265ab53370c12972798902aba42bc" | "integration test pass, QG fail" | 20      | 11               | Failed          | Task_Failure   | Task_Successful     | Quality_Gate_Failed     | "BUILD SUCCESSFUL"     | ""                     | "BUILD FAILED"     | "Code coverage failed"
//        "4a60a14dae0265ab53370c12972798902aba42bc" | "integration test pass, QG pass" | 20      | 11               | Successful      | Not_Applicable | Task_Successful     | Task_Successful         | "BUILD SUCCESSFUL"     | ""                     | "BUILD SUCCESSFUL" | ""
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
