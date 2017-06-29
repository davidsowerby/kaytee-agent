package uk.q3c.kaytee.agent.build

import spock.lang.Specification
import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.remote.GPIssue
import uk.q3c.build.gitplus.remote.GitRemote
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.BuildRunner
import uk.q3c.kaytee.plugin.TaskKey

import java.time.OffsetDateTime

/**
 * Created by David Sowerby on 28 Jun 2017
 */
class DefaultIssueCreatorTest extends Specification {

    DefaultIssueCreator creator
    BuildRecordCollator buildRecordCollator = Mock(BuildRecordCollator)
    GitPlus gitPlus = Mock(GitPlus)
    GitRemote gitRemote = Mock(GitRemote)
    Build build = Mock(Build)
    Project project = Mock(Project)
    final String userName = 'davidsowerby'
    final String projectName = 'wiggly'
    BuildRunner buildRunner = Mock(BuildRunner)
    UUID uid = UUID.randomUUID()
    BuildRecord buildRecord
    String failureDescription = "It broke"

    def setup() {
        creator = new DefaultIssueCreator(buildRecordCollator, gitPlus)
        gitPlus.remote >> gitRemote
        build.project >> project
        project.remoteUserName >> userName
        project.shortProjectName >> projectName
        build.buildRunner >> buildRunner
        buildRunner.uid >> uid
        buildRecord = new BuildRecord(uid, OffsetDateTime.now(), false)
        gitRemote.remoteRepoFullName() >> "$userName/$projectName"
    }

    def "raise issue succeeds returns a new issue, failed task"() {
        given:
        buildRecord.causeOfFailure = BuildFailCauseKey.Task_Failure
        buildRecord.failureDescription = failureDescription
        buildRecord.failedTask = TaskKey.Integration_Test

        when:
        GPIssue issue = creator.raiseIssue(build)

        then:
        1 * gitPlus.useRemoteOnly(userName, projectName)

        then:
        1 * gitPlus.execute()
        buildRecordCollator.getRecord(uid) >> buildRecord

        then:
        1 * gitRemote.createIssue("KayTee build: Integration Test failed", failureDescription, "bug") >> new GPIssue(23)
        issue.number == 23

    }

    def "raise issue succeeds returns a new issue, non task failure"() {
        given:
        buildRecord.causeOfFailure = BuildFailCauseKey.Preparation_Failed
        buildRecord.failureDescription = failureDescription
        buildRecord.failedTask = TaskKey.Custom

        when:
        GPIssue issue = creator.raiseIssue(build)

        then:
        1 * gitPlus.useRemoteOnly(userName, projectName)

        then:
        1 * gitPlus.execute()
        buildRecordCollator.getRecord(uid) >> buildRecord

        then:
        1 * gitRemote.createIssue("KayTee build: Preparation Failed", failureDescription, "bug") >> new GPIssue(23)
        issue.number == 23

    }

    def "raise issue fails, returns issue number 0"() {
        given:
        buildRecord.causeOfFailure = BuildFailCauseKey.Preparation_Failed
        buildRecord.failureDescription = failureDescription
        buildRecord.failedTask = TaskKey.Custom

        when:
        GPIssue issue = creator.raiseIssue(build)

        then:
        1 * gitPlus.useRemoteOnly(userName, projectName)

        then:
        1 * gitPlus.execute()
        buildRecordCollator.getRecord(uid) >> buildRecord

        then:
        1 * gitRemote.createIssue("KayTee build: Preparation Failed", failureDescription, "bug") >> {
            throw new IOException()
        }
        issue.number == 0
    }
}
