package uk.q3c.kaytee.agent.system

import org.apache.commons.codec.digest.DigestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.build.BuildFactory
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.BuildRunner
import uk.q3c.kaytee.agent.queue.DefaultBuildRunner
/**
 * Created by David Sowerby on 14 Jan 2017
 */
class DefaultInstallationInfoTest extends Specification {

    DefaultInstallationInfo info
    Build build = Mock(Build)
    BuildRunner buildRequest
    Project project = Mock(Project)
    String sha = DigestUtils.sha1Hex('x')
    BuildFactory buildFactory = Mock(BuildFactory)
    GlobalBusProvider globalBusProvider = Mock(GlobalBusProvider)

    @Rule
    TemporaryFolder temporaryFolder
    File temp

    void setup() {
        info = new DefaultInstallationInfo()
        temp = temporaryFolder.getRoot()

    }

    def "directories and files, development mode false"() {
        given:
        System.setProperty(ConstantsKt.developmentMode_propertyName, "false")
        project.shortProjectName >> 'wiggly'
        buildRequest = new DefaultBuildRunner(buildFactory, globalBusProvider, false, "", new GitSHA(sha), project, UUID.randomUUID())
        buildFactory.create(buildRequest, false) >> build
        build.buildNumber() >> 12
        build.buildRunner >> buildRequest


        expect:

        info.gradleOutputDir(build) == new File('/home/david/kaytee-data/wiggly/12/build-output')
        info.projectDir(build) == new File('/home/david/kaytee-data/wiggly')
        info.buildNumberDir(build) == new File('/home/david/kaytee-data/wiggly/12')
        info.projectInstanceDir(build) == new File('/home/david/kaytee-data/wiggly/12/wiggly')

        info.gradleStdErrFile(build) == new File('/home/david/kaytee-data/wiggly/12/build-output/stderr.txt')
        info.gradleStdOutFile(build) == new File('/home/david/kaytee-data/wiggly/12/build-output/stdout.txt')
    }

    def "directories and files, development mode true"() {
        given:
        System.setProperty(ConstantsKt.developmentMode_propertyName, "true")
        System.setProperty(ConstantsKt.baseDir_propertyName, temp.getAbsolutePath())
        project.shortProjectName >> 'wiggly'
        buildRequest = new DefaultBuildRunner(buildFactory, globalBusProvider, false, "", new GitSHA(sha), project, UUID.randomUUID())
        buildFactory.create(buildRequest, false) >> build
        build.buildNumber() >> 12
        build.buildRunner >> buildRequest


        expect:

        info.gradleOutputDir(build) == new File(temp, 'wiggly/12/build-output')
        info.projectDir(build) == new File(temp, 'wiggly')
        info.buildNumberDir(build) == new File(temp, 'wiggly/12')
        info.projectInstanceDir(build) == new File(temp, 'wiggly/12/wiggly')

        info.gradleStdErrFile(build) == new File(temp, 'wiggly/12/build-output/stderr.txt')
        info.gradleStdOutFile(build) == new File(temp, 'wiggly/12/build-output/stdout.txt')
    }
}
