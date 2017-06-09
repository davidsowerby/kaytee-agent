package uk.q3c.kaytee.agent

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.jackson.Jackson
import ratpack.test.embed.EmbeddedApp
import spock.lang.Unroll
import uk.q3c.kaytee.agent.api.BuildRequest
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.app.SubscriptionRequest
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.build.TaskResult
import uk.q3c.rest.hal.HalMapper

import java.time.LocalDateTime

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Not_Applicable
import static uk.q3c.kaytee.agent.i18n.BuildStateKey.Successful
import static uk.q3c.kaytee.agent.i18n.TaskResultStateKey.Task_Successful
import static uk.q3c.kaytee.plugin.TaskKey.*
/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class FunctionalTest1 extends FunctionalTestBase {

    final static timeout = 20 // seconds per task
    static List<BuildRecord> subscriberMessages
    static LocalDateTime timeoutAt

    EmbeddedApp subscriber = GroovyEmbeddedApp.ratpack {
        bindings {
            add(ObjectMapper.class, new HalMapper())
        }
        handlers {
            all {
                context.parse(Jackson.fromJson(BuildRecord.class)).then { buildRecord ->
                    subscriberMessages.add(buildRecord)
                    timeoutAt = LocalDateTime.now().plusSeconds(timeout)
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

    /**
     * Tests for all conditions of build stopping
     * @return
     */

    private boolean buildStopped() {
        if (LocalDateTime.now().isAfter(timeoutAt)) {
            return true
        }
        return finalRecord().hasCompleted()
    }

    private BuildRecord finalRecord() {
        if (subscriberMessages.isEmpty()) {
            throw new IllegalArgumentException("No messages have been received")
        }
        return subscriberMessages.get(subscriberMessages.size() - 1)
    }

    @Unroll
    def "run build #testDesc"() {
        given:
        defaultSubscribe()
        final String fullProjectName = "davidsowerby/kaytee-test"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, commitId)
        timeoutAt = LocalDateTime.now().plusSeconds(timeout)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(ConstantsKt.buildRequests)
        while (!buildStopped()) {
            println "Waiting for build to complete"
            Thread.sleep(1000)
        }

        then:
        BuildRecord finalRecord = finalRecord()
        TaskResult unitTestResult = finalRecord.taskResult(Unit_Test)
        TaskResult changeLogResult = finalRecord.taskResult(Generate_Change_Log)
        TaskResult buildInfoResult = finalRecord.taskResult(Generate_Build_Info)
        TaskResult publishToLocalResult = finalRecord.taskResult(Local_Publish)
        TaskResult integrationTestResult = finalRecord.taskResult(Integration_Test)
        TaskResult functionalTestResult = finalRecord.taskResult(Functional_Test)
        TaskResult acceptanceTestResult = finalRecord.taskResult(Acceptance_Test)
        TaskResult bintrayUploadResult = finalRecord.taskResult(Bintray_Upload)
        TaskResult mergeToMasterResult = finalRecord.taskResult(Merge_to_Master)
        TaskResult productionTestResult = finalRecord.taskResult(Production_Test)






        finalRecord.failureDescription == ""

        finalRecord.state == finalBuildState
        finalRecord.causeOfFailure == causeOfFailure

        unitTestResult.outcome == unitTestExpected
        unitTestResult.completedAt.isBefore(finalRecord.buildCompletedAt) || unitTestResult.completedAt.isEqual(finalRecord.buildCompletedAt)
        unitTestResult.stdOut.contains(unitStdOut)
        unitTestResult.stdErr.contains(unitStdErr)


        integrationTestResult.outcome == integrationTestExpected
        integrationTestResult.completedAt.isBefore(finalRecord.buildCompletedAt) || integrationTestResult.completedAt.isEqual(finalRecord.buildCompletedAt)
        integrationTestResult.stdOut.contains(iTestStdOut)
        integrationTestResult.stdErr.contains(iTestStdErr)

        buildInfoResult.passed()
        changeLogResult.passed()
        publishToLocalResult.passed()
        functionalTestResult.passed()
        acceptanceTestResult.passed()
        productionTestResult.passed()
        bintrayUploadResult.notRun()
        mergeToMasterResult.notRun()



        where:
        commitId                                   | testDesc                    | expectedMessages | finalBuildState | causeOfFailure | unitTestExpected | integrationTestExpected | unitStdOut         | unitStdErr | iTestStdOut        | iTestStdErr
        "2e2e31505dc8ccd27300a63eae07ebf43ea4eede" | "full cycle, simple tests " | 29               | Successful      | Not_Applicable | Task_Successful  | Task_Successful         | "BUILD SUCCESSFUL" | ""         | "BUILD SUCCESSFUL" | ""
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
