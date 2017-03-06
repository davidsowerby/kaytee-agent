package uk.q3c.simplycd.lifecycle.prepare

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.local.CloneExistsResponse
import uk.q3c.simplycd.agent.i18n.LabelKey
import uk.q3c.simplycd.agent.i18n.NamedFactory
import uk.q3c.simplycd.build.Build
import uk.q3c.simplycd.build.BuildPreparationException
import uk.q3c.simplycd.i18n.Named
import uk.q3c.simplycd.system.InstallationInfo

/**
 * Created by David Sowerby on 18 Jan 2017
 */
class DefaultGitClone @Inject constructor(val installationInfo: InstallationInfo, val gitPlus: GitPlus, namedFactory: NamedFactory)
    : GitClone, Named by namedFactory.create(LabelKey.Git_Clone) {


    private val log = LoggerFactory.getLogger(this.javaClass.name)


    override fun execute(build: Build) {
        log.debug("executing GitClone preparation step for '{}'", build.buildRequest.project.name)
        try {
            gitPlus.local
                    .create(true)
                    .cloneExistsResponse(CloneExistsResponse.EXCEPTION)
                    .projectDirParent(installationInfo.codeDir(build))
            gitPlus.remote
                    .repoUser(build.project.remoteUserName)
                    .repoName(build.project.name)
            gitPlus.execute()

            log.debug("Cloning from '{}'", gitPlus.remote.remoteRepoFullName())
            gitPlus.local.cloneRemote()


            gitPlus.local.checkoutCommit(build.gitHash)
            log.debug("Checked out commit {}", build.gitHash)
        } catch (e: Exception) {
            val msg = "Git clone operation failed"
            throw BuildPreparationException(msg, e)
        }
    }
}