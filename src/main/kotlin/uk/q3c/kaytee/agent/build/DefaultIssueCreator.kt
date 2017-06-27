package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.remote.GPIssue

/**
 * Created by David Sowerby on 19 Jun 2017
 */
class DefaultIssueCreator @Inject constructor(val buildRecordCollator: BuildRecordCollator,
                                              val gitPlus: GitPlus) : IssueCreator {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override fun raiseIssue(build: Build): GPIssue {
        log.debug("raising issue for build failure, build $build")
        gitPlus.local.active = false
        gitPlus.remote
                .repoUser(build.project.remoteUserName)
                .repoName(build.project.shortProjectName)
        gitPlus.execute()
        val buildRecord = buildRecordCollator.getRecord(build.buildRunner.uid)
        val issueTitle = "KayTee build: ${buildRecord.causeOfFailure}"
        val issueBody = buildRecord.failureDescription
        val newIssue = gitPlus.remote.createIssue(issueTitle, issueBody, "bug")
        log.info("New issue ${newIssue.number} raised in ${gitPlus.remote.remoteRepoFullName()}")
        return newIssue

    }
}