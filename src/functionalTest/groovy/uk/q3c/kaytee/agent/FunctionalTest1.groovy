package uk.q3c.kaytee.agent

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.apache.commons.io.FileUtils
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
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.rest.hal.HalMapper

import java.time.LocalDateTime

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Not_Applicable
import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Task_Failure
import static uk.q3c.kaytee.agent.i18n.BuildStateKey.Failed
import static uk.q3c.kaytee.agent.i18n.BuildStateKey.Successful
import static uk.q3c.kaytee.plugin.TaskKey.*

/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class FunctionalTest1 extends FunctionalTestBase {

    final static timeout = 20 // seconds per task
    static List<BuildRecord> subscriberMessages
    static UUID buildId
    static boolean buildComplete = false
    static LocalDateTime timeoutAt
    static BuildRecord finalRecord

    EmbeddedApp subscriber = GroovyEmbeddedApp.ratpack {
        bindings {
            add(ObjectMapper.class, new HalMapper())
        }
        handlers {
            all {
                context.parse(Jackson.fromJson(BuildRecord.class)).then { buildRecord ->
                    if (subscriberMessages.isEmpty()) {
                        // first build request must be the one we want
                        buildId = buildRecord.uid
                    }
                    if (buildRecord.uid == buildId) {
                        if (buildRecord.hasCompleted()) {
                            buildComplete = true
                            finalRecord = buildRecord
                        }
                    }
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
        buildId = null
        buildComplete = false
        finalRecord = null
        subscriberUri = subscriber.address.toString()
    }

    def cleanup() {
        StringBuilder buf = new StringBuilder()
        for (msg in subscriberMessages) {
            buf.append(msg.summary())
        }
        FileUtils.writeStringToFile(new File("messages.txt"), buf.toString())
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
        return buildComplete
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
        ReceivedResponse response = post(ConstantsKt.buildRequests)
        String t = response.getBody()
        println t
        while (!buildStopped()) {
            println "Waiting for build to complete"
            Thread.sleep(1000)
        }

        then:

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


        finalRecord.failureDescription.contains(failDesc)

        finalRecord.state == finalBuildState
        finalRecord.causeOfFailure == causeOfFailure

        unitTestResult.state == unitTestExpected
        unitTestResult.completedAt.isBefore(finalRecord.completedAt) || unitTestResult.completedAt.isEqual(finalRecord.completedAt)
        unitTestResult.stdOut.contains(unitStdOut)
        unitTestResult.stdErr.contains(unitStdErr)


        integrationTestResult.state == integrationTestExpected
        integrationTestResult.completedAt.isBefore(finalRecord.completedAt) || integrationTestResult.completedAt.isEqual(finalRecord.completedAt)
        integrationTestResult.stdOut.contains(iTestStdOut)
        integrationTestResult.stdErr.contains(iTestStdErr)

        buildInfoResult.passed() == expBuildInfo
        changeLogResult.passed() == expChangeLog
        publishToLocalResult.passed() == expPublishLocal
        functionalTestResult.passed() == expFunc
        acceptanceTestResult.passed() == expAccept
        productionTestResult.passed() == expProd
        bintrayUploadResult.notRun() == expBintray
        mergeToMasterResult.notRun() == expMerge



        where:
        commitId                                   | testDesc                                         | finalBuildState | causeOfFailure | unitTestExpected        | integrationTestExpected | unitStdOut         | unitStdErr    | iTestStdOut        | iTestStdErr | expBuildInfo | expChangeLog | expPublishLocal | expFunc | expAccept | expProd | expBintray | expMerge | failDesc
        "e9eca1cb86d64b355027952c107fe25e2e2e59f0" | "full cycle, except bintray and merge, all pass" | Successful      | Not_Applicable | TaskStateKey.Successful | TaskStateKey.Successful | "BUILD SUCCESSFUL" | ""            | "BUILD SUCCESSFUL" | ""          | true         | true         | true            | true    | true      | false   | true       | true     | ""
        "e85023ca7c5e411af90b91cf46dbf60e37f89a07" | "unit test failure"                              | Failed          | Task_Failure   | TaskStateKey.Failed     | TaskStateKey.Not_Run    | "FAILURE"          | "test FAILED" | ""                 | ""          | false        | false        | false           | false   | false     | false   | true       | true     | "There were failing tests"
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
