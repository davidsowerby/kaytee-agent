package uk.q3c.simplycd.agent.prepare

import uk.q3c.build.gitplus.gitplus.GitPlus
import uk.q3c.build.gitplus.local.CloneExistsResponse
import uk.q3c.build.gitplus.local.GitLocal
import uk.q3c.build.gitplus.local.GitLocalException
import uk.q3c.build.gitplus.remote.GitRemote
import uk.q3c.simplycd.agent.i18n.LabelKey
import uk.q3c.simplycd.build.BuildPreparationException
import uk.q3c.simplycd.lifecycle.prepare.DefaultGitClone

/**
 * Created by David Sowerby on 18 Jan 2017
 */
class DefaultGitCloneTest extends PreparationStepSpecification {

    DefaultGitClone gitClone
    GitPlus gitPlus = Mock(GitPlus)
    GitLocal gitLocal = Mock(GitLocal)
    GitRemote gitRemote = Mock(GitRemote)

    def setup() {
        gitPlus.local >> gitLocal
        gitPlus.remote >> gitRemote
        gitClone = new DefaultGitClone(installationInfo, gitPlus, i18NNamedFactory)
    }

    def "invoke successfully"() {
        when:
        gitClone.execute(build)

        then:
        1 * gitLocal.create(true) >> gitLocal
        1 * gitLocal.cloneExistsResponse(CloneExistsResponse.EXCEPTION) >> gitLocal
        1 * gitLocal.projectDirParent(codeDir) >> gitLocal
        1 * gitRemote.repoUser(repoUserName) >> gitRemote
        1 * gitRemote.repoName(projectName) >> gitRemote

        then:
        1 * gitPlus.execute()
        1 * gitLocal.cloneRemote()
        1 * gitLocal.checkoutCommit(gitHash)
    }

    def "clone fails"() {
        when:
        gitClone.execute(build)

        then:
        1 * gitLocal.create(true) >> gitLocal
        1 * gitLocal.cloneExistsResponse(CloneExistsResponse.EXCEPTION) >> gitLocal
        1 * gitLocal.projectDirParent(codeDir) >> gitLocal
        1 * gitRemote.repoUser(repoUserName) >> gitRemote
        1 * gitRemote.repoName(projectName) >> gitRemote

        then:
        1 * gitPlus.execute()
        1 * gitLocal.cloneRemote() >> { throw new GitLocalException("failed") }
        0 * gitLocal.checkoutCommit(gitHash)
        thrown BuildPreparationException
    }

    def "remote user name not set"() {
        when:
        gitClone.execute(build)

        then:
        0 * gitPlus.execute()
        0 * gitLocal.cloneRemote() >> { throw new GitLocalException("failed") }
        0 * gitLocal.checkoutCommit(gitHash)
        thrown BuildPreparationException
    }

    def "name"() {

        when:
        gitClone = new DefaultGitClone(installationInfo, gitPlus, i18NNamedFactory)

        then:
        1 * i18NNamedFactory.create(LabelKey.Git_Clone)
    }
}
