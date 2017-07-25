package uk.q3c.kaytee.agent

import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import spock.lang.Unroll
import uk.q3c.build.gitplus.GitPlusFactory
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.local.GitBranch
import uk.q3c.build.gitplus.local.Tag
import uk.q3c.kaytee.agent.api.BuildRequest
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.build.TaskResult
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.plugin.TaskKey

import java.time.Duration
import java.time.LocalDateTime

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.*
import static uk.q3c.kaytee.agent.i18n.TaskStateKey.*
import static uk.q3c.kaytee.plugin.TaskKey.*

//import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.*
//import static uk.q3c.kaytee.agent.i18n.TaskStateKey.*
//import static uk.q3c.kaytee.agent.i18n.BuildStateKey.*
/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class FunctionalTest extends FunctionalTestBase {

    GitPlus gitPlus


    def setup() {
        gitPlus = GitPlusFactory.instance
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
    def "scenario #testDesc"() {
        given:
        timeoutPeriod = 18000 // 5 mins
        defaultSubscribe()
        BuildRequest buildRequest = new BuildRequest("davidsowerby/kaytee-test", commitId)
        timeoutAt = LocalDateTime.now().plusSeconds(timeoutPeriod)

        String shortSha = new GitSHA(commitId).short()
        File buildOutputDir = new File(dataArea, "kaytee-test/$shortSha/build-output")
        File stdErr = new File(buildOutputDir, "stderr.txt")
        File stdOut = new File(buildOutputDir, "stdout.txt")
        File buildRecord = new File(buildOutputDir, "buildRecord.json")
        File stacktrace = new File(buildOutputDir, "stacktrace.txt")
        File buildInfo = new File(buildOutputDir, "buildInfo.txt")


        when:
        submitRequest(buildRequest)
        ReceivedResponse response = post(ConstantsKt.buildRequests)
        String t = response.getBody()
        println t
        while (!buildStopped()) {
            int togo = Duration.between(LocalDateTime.now(), timeoutAt).seconds
            println "Waiting for build to complete, timeout in $togo seconds    "
            Thread.sleep(1000)
        }
        then:
        !timedOut
        finalRecord.state == BuildStateKey.Complete
        finalRecord.outcome == outcome
        finalRecord.causeOfFailure == causeOfFailure
        finalRecord.failureDescription.contains(failDesc)

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

        // buildOutput
        stdErr.exists()
        stdOut.exists()
        buildRecord.exists()
        stacktrace.exists()
        buildInfo.exists()

        where:

        testDesc                    | commitId                                   | baseVersion | outcome                  | causeOfFailure | unitTestExpected | integrationTestExpected | unitStdOut                    | unitStdErr   | iTestStdOut        | iTestStdErr | expBuildInfo | expChangeLog | expPublishLocal | expFunc | expAccept | expProd | expBintray | expMerge | expTagged | failDesc
        "full cycle all steps pass" | "7ac2e38d98118837fd65fea5f32e2ef8b49cca53" | "0.6.3.0"   | BuildStateKey.Successful | Not_Applicable | Successful       | Successful              | "BUILD SUCCESSFUL"            | ""           | "BUILD SUCCESSFUL" | ""          | true         | true         | true            | true    | true      | false   | false      | false    | true      | ""
        "unit test fails"           | "cd83ed3822f36dd3ce6388c3e31018df03654af2" | "0.6.4.0"   | BuildStateKey.Failed     | Task_Failure   | Failed           | Not_Run                 | "3 tests completed, 1 failed" | "Add FAILED" | ""                 | ""          | false        | false        | false           | false   | false     | false   | false      | false    | false     | "There were failing tests"
    }


    private void configureGitPlus(String commitId) {
        gitPlus.remote.active = false
        gitPlus.local.projectName = "kaytee-test"
        String commitIdShort = new GitSHA(commitId).short()
        // project dir
        File projectDirParent = new File(dataArea, "kaytee-test/$commitIdShort")
        gitPlus.local.projectDirParent = projectDirParent
        gitPlus.execute()
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
}
