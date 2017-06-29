package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.remote.GPIssue
import uk.q3c.kaytee.agent.i18n.BuildFailCauseKey

/**
 * Created by David Sowerby on 19 Jun 2017
 */
class DefaultIssueCreator @Inject constructor(val buildRecordCollator: BuildRecordCollator,
                                              val gitPlus: GitPlus) : IssueCreator {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun raiseIssue(build: Build): GPIssue {
        log.debug("raising issue for build failure, build $build")
        try {
            gitPlus.useRemoteOnly(build.project.remoteUserName, build.project.shortProjectName)
            gitPlus.execute()
            val buildRecord = buildRecordCollator.getRecord(build.buildRunner.uid)
            val subTitle = if (buildRecord.causeOfFailure == BuildFailCauseKey.Task_Failure) {
                "${buildRecord.failedTask.name.replace("_", " ")} failed"
            } else {
                buildRecord.causeOfFailure.name.replace("_", " ")
            }
            val issueTitle = "KayTee build: $subTitle"
            val issueBody = buildRecord.failureDescription
            val newIssue = gitPlus.remote.createIssue(issueTitle, issueBody, "bug")
            log.info("New issue ${newIssue.number} raised in ${gitPlus.remote.remoteRepoFullName()}")
            return newIssue
        } catch (e: Exception) {
            log.warn("Failed to create issue", e)
            return GPIssue(0)
        }

    }
}