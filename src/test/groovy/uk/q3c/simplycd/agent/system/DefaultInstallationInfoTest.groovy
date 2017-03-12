package uk.q3c.simplycd.agent.system

import org.apache.commons.codec.digest.DigestUtils
import spock.lang.Specification
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.build.BuildFactory
import uk.q3c.simplycd.agent.queue.BuildRequest
import uk.q3c.simplycd.agent.queue.DefaultBuildRequest
import uk.q3c.simplycd.project.Project

/**
 * Created by David Sowerby on 14 Jan 2017
 */
class DefaultInstallationInfoTest extends Specification {

    DefaultInstallationInfo info
    Build build = Mock(Build)
    BuildRequest buildRequest
    Project project = Mock(Project)
    String sha = DigestUtils.sha1Hex('x')
    BuildFactory buildFactory = Mock(BuildFactory)

    void setup() {
        info = new DefaultInstallationInfo()


    }

    def "directories and files"() {
        given:
        project.shortProjectName >> 'wiggly'
        buildRequest = new DefaultBuildRequest(buildFactory, new GitSHA(sha), project, UUID.randomUUID())
        buildFactory.create(buildRequest) >> build
        build.buildNumber() >> 12
        build.buildRequest >> buildRequest

        expect:

        info.gradleOutputDir(build) == new File('/home/david/simplycd-data/wiggly/12/build-output')
        info.codeDir(build) == new File('/home/david/simplycd-data/wiggly/12/code')
        info.gradleStdErrFile(build) == new File('/home/david/simplycd-data/wiggly/12/build-output/stderr.txt')
        info.gradleStdOutFile(build) == new File('/home/david/simplycd-data/wiggly/12/build-output/stdout.txt')

    }
}
