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
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.rest.hal.HalMapper

import java.time.Duration
import java.time.LocalDateTime

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Not_Applicable
import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.Task_Failure
import static uk.q3c.kaytee.agent.i18n.TaskStateKey.*
import static uk.q3c.kaytee.plugin.TaskKey.*

/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class FunctionalTest1 extends FunctionalTestBase {

    static timeoutPeriod = 20 // seconds per task
    static List<BuildRecord> subscriberMessages
    static UUID buildId
    static boolean buildComplete = false
    static LocalDateTime timeoutAt
    static BuildRecord finalRecord

    File tempDataArea

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
                    timeoutAt = LocalDateTime.now().plusSeconds(timeoutPeriod)
                }
//TODO response    WARN  r.s.internal.NettyHandlerAdapter - No response sent for PUT request to / (last handler: closure at line 40 of FunctionalTest1.groovy)
            }
        }
    }

    String subscriberUri
    boolean timedOut
//    HttpClient httpClient

    def setup() {
        tempDataArea = new File(temp, "kaytee-data")
        subscriberMessages = new ArrayList<>()
        buildId = null
        buildComplete = false
        finalRecord = null
        timedOut = false
        subscriberUri = subscriber.address.toString()
        System.setProperty(ConstantsKt.baseDir_propertyName, tempDataArea.absolutePath)


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
            timedOut = true
            return true
        }
        return buildComplete
    }

    @Unroll
    def "run build #testDesc"() {
        given:
        System.setProperty(ConstantsKt.baseDir_propertyName, tempDataArea.absolutePath)
        timeoutPeriod = 20
        defaultSubscribe()
        final String fullProjectName = "davidsowerby/kaytee-test"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, commitId)
        timeoutAt = LocalDateTime.now().plusSeconds(timeoutPeriod)



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
        TaskResult publishToLocalResult = finalRecord.taskResult(Publish_to_Local)
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
        bintrayUploadResult.notRequired() == expBintray
        mergeToMasterResult.notRequired() == expMerge



        where:
        commitId                                   | testDesc                                         | finalBuildState          | causeOfFailure | unitTestExpected | integrationTestExpected | unitStdOut         | unitStdErr | iTestStdOut        | iTestStdErr | expBuildInfo | expChangeLog | expPublishLocal | expFunc | expAccept | expProd | expBintray | expMerge | failDesc
        "bd3babdbff16a9ab3b68d000b4375a4a98392375" | "full cycle, except bintray and merge, all pass" | BuildStateKey.Successful | Not_Applicable | Successful       | Successful              | "BUILD SUCCESSFUL" | ""         | "BUILD SUCCESSFUL" | ""          | true         | true         | true            | true    | true      | false   | true       | true     | ""
        "dcc4b5ebbd9ec8ecfe1103a0f672a5ae30d6b62b" | "unit test failure"                              | BuildStateKey.Failed     | Task_Failure   | Failed           | Not_Run                 | ""                 | ""         | ""                 | ""          | false        | false        | false           | false   | false     | false   | true       | true     | "There were failing tests"
//        "5771e944c6e3d32072962a1edfab37bd4192fad6" | "version check failure"                          | BuildStateKey.Failed     | Preparation_Failed | Not_Run          | Not_Run                 | ""                 | ""            | ""                 | ""          | false        | false        | false           | false   | false     | false   | false      | false    | "Preparation failure"

    }

    //cbe99aaaf6fa74c249d0fdb74a38b5dab8fc4ca2


    def "gitPlus"() {
        given:
        timeoutPeriod = 18000 // 5 mins
        defaultSubscribe()
        final String fullProjectName = "davidsowerby/gitPlus"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, "f5f8f0ecde59abb69d8b534a9735e625995df333")
        timeoutAt = LocalDateTime.now().plusSeconds(timeoutPeriod)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        ReceivedResponse response = post(ConstantsKt.buildRequests)
        String t = response.getBody()
        println t
        while (!buildStopped()) {
            int togo = Duration.between(LocalDateTime.now(), timeoutAt).seconds
            println "Waiting for build to complete, timeout in $togo seconds "
            Thread.sleep(1000)
        }
        then:
        !timedOut
        finalRecord.state == BuildStateKey.Successful
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
