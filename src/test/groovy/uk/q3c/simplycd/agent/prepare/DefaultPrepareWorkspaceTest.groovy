package uk.q3c.simplycd.agent.prepare

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.simplycd.build.Build
import uk.q3c.simplycd.i18n.NamedFactory
import uk.q3c.simplycd.lifecycle.prepare.DefaultPrepareWorkspace
import uk.q3c.simplycd.lifecycle.prepare.PrepareWorkspace
import uk.q3c.simplycd.system.InstallationInfo

/**
 * Created by David Sowerby on 19 Jan 2017
 */
class DefaultPrepareWorkspaceTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp

    PrepareWorkspace step
    InstallationInfo installationInfo = Mock(InstallationInfo)
    NamedFactory i18NNamedFactory = Mock(NamedFactory)
    Build build = Mock(Build)
    File codeBuildDir

    void setup() {
        temp = temporaryFolder.getRoot()
        codeBuildDir = new File(temp, "codeDir")
        installationInfo.codeDir(build) >> codeBuildDir
        step = new DefaultPrepareWorkspace(installationInfo, i18NNamedFactory)
    }

    def "Execute"() {
        when:
        step.execute(build)

        then:
        codeBuildDir.exists()
    }
}
