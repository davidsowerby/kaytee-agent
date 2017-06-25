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
import uk.q3c.build.gitplus.GitPlusFactory
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.local.GitBranch
import uk.q3c.build.gitplus.local.Tag
import uk.q3c.kaytee.agent.api.BuildRequest
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.app.SubscriptionRequest
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.build.TaskResult
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.plugin.TaskKey
import uk.q3c.rest.hal.HalMapper

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
    GitPlus gitPlus
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
        gitPlus = GitPlusFactory.instance
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
        Thread.sleep(1000)

        then:
        configureGitPlus(commitId)
        TaskResult unitTestResult = finalRecord.taskResult(Unit_Test)
        TaskResult changeLogResult = finalRecord.taskResult(Generate_Change_Log)
        TaskResult buildInfoResult = finalRecord.taskResult(Generate_Build_Info)
        TaskResult publishToLocalResult = finalRecord.taskResult(Publish_to_Local)
        TaskResult integrationTestResult = finalRecord.taskResult(Integration_Test)
        TaskResult functionalTestResult = finalRecord.taskResult(Functional_Test)
        TaskResult acceptanceTestResult = finalRecord.taskResult(Acceptance_Test)
        TaskResult productionTestResult = finalRecord.taskResult(Production_Test)
        TaskResult tagResult = finalRecord.taskResult(TaskKey.Tag)  // full Tag reference needed to avoid Groovy getting confused
        TaskResult bintrayUploadResult = finalRecord.taskResult(Bintray_Upload)
        TaskResult mergeToMasterResult = finalRecord.taskResult(Merge_to_Master)



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
        bintrayUploadResult.passed() == expBintray
        mergeToMasterResult.passed() == expMerge
        tagResult.passed() == expTagged
        if (expTagged) {
            hasTag(commitId, baseVersion)
        } else {
            !hasTag(commitId, baseVersion)
        }
        if (expMerge) {
            masterMerged(commitId)
        } else {
            !masterMerged(commitId)
        }


        where:
        commitId                                   | baseVersion | testDesc                    | finalBuildState          | causeOfFailure | unitTestExpected | integrationTestExpected | unitStdOut         | unitStdErr | iTestStdOut        | iTestStdErr | expBuildInfo | expChangeLog | expPublishLocal | expFunc | expAccept | expProd | expBintray | expMerge | expTagged | failDesc
        "097ae697e753ea70c9f79f5c157062f3a6e1abf1" | "0.5.2.0"   | "full cycle all steps pass" | BuildStateKey.Successful | Not_Applicable | Successful       | Successful              | "BUILD SUCCESSFUL" | ""         | "BUILD SUCCESSFUL" | ""          | true         | true         | true            | true    | true      | false   | true       | true     | true      | ""
        "230ca897053f8f934565057538bc5dd2ab66afe9" | "0.5.3.0"   | "unit test failure"         | BuildStateKey.Failed     | Task_Failure   | Failed           | Not_Run                 | ""                 | ""         | ""                 | ""          | false        | false        | false           | false   | false     | false   | false      | false    | false     | "There were failing tests"
//        "5771e944c6e3d32072962a1edfab37bd4192fad6" | "version check failure"                          | BuildStateKey.Failed     | Preparation_Failed | Not_Run          | Not_Run                 | ""                 | ""            | ""                 | ""          | false        | false        | false           | false   | false     | false   | false      | false    | "Preparation failure"

    }

    boolean masterMerged(String commitId) {
        GitSHA expectedSha = new GitSHA(commitId)
        GitBranch masterBranch = new GitBranch('master')
        GitBranch developBranch = new GitBranch('develop')
        if (gitPlus.local.headCommitSHA(masterBranch) == expectedSha) {
            if (gitPlus.local.headCommitSHA(developBranch) == expectedSha) {
                return true
            }
        }
        return false
    }

    boolean hasTag(String commitId, String baseVersion) {
        GitSHA gitSha = new GitSHA(commitId)
        String version = "$baseVersion.${gitSha.short()}"
        List<Tag> tags = gitPlus.local.tags()
        for (tag in tags) {
            if (tag.tagName == version) {
                if (tag.commit.hash == commitId) {
                    return true
                }
            }
        }
        return false
    }
    //cbe99aaaf6fa74c249d0fdb74a38b5dab8fc4ca2


    private void defaultSubscribe() {
        subscribe("http://localhost:9001/buildRecords", subscriberUri)
    }

    private void configureGitPlus(String commitId) {
        gitPlus.remote.active = false
        gitPlus.local.projectName = "kaytee-test"
        String commitIdShort = new GitSHA(commitId).short()
        // project dir
        File projectDirParent = new File(tempDataArea, "kaytee-test/$commitIdShort")
        gitPlus.local.projectDirParent = projectDirParent
        gitPlus.execute()
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
