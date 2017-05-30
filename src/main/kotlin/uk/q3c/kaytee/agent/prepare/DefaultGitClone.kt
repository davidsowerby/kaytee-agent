package uk.q3c.kaytee.agent.prepare

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.local.CloneExistsResponse
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.build.BuildPreparationException
import uk.q3c.kaytee.agent.i18n.LabelKey
import uk.q3c.kaytee.agent.i18n.Named
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.system.InstallationInfo

/**
 * Created by David Sowerby on 18 Jan 2017
 */
class DefaultGitClone @Inject constructor(val installationInfo: InstallationInfo, val gitPlus: GitPlus, namedFactory: NamedFactory)
    : GitClone, Named by namedFactory.create(LabelKey.Git_Clone) {


    private val log = LoggerFactory.getLogger(this.javaClass.name)


    override fun execute(build: Build) {
        log.debug("executing GitClone preparation step for '{}'", build.buildRunner.project.shortProjectName)
        try {
            gitPlus.local
                    .projectDirParent(installationInfo.buildNumberDir(build))
                    .cloneFromRemote(true)
                    .cloneExistsResponse(CloneExistsResponse.EXCEPTION)
            gitPlus.remote
                    .repoUser(build.project.remoteUserName)
                    .repoName(build.project.shortProjectName)
            gitPlus.execute()
            log.debug("Cloning complete")

            gitPlus.local.checkoutCommit(build.gitHash, "kaytee")
            log.debug("Checked out commit {} to new branch 'kaytee'", build.gitHash)
        } catch (e: Exception) {
            val msg = "Git clone or checkout operation failed"
            throw BuildPreparationException(msg, e)
        }
    }
}