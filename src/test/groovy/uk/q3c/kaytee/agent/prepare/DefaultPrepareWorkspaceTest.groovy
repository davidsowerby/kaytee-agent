package uk.q3c.kaytee.agent.prepare

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.BuildRunner
import uk.q3c.kaytee.agent.system.DefaultInstallationInfo
import uk.q3c.kaytee.agent.system.InstallationInfo

/**
 * Created by David Sowerby on 19 Jan 2017
 */
class DefaultPrepareWorkspaceTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp

    PrepareWorkspace step
    InstallationInfo installationInfo
    NamedFactory i18NNamedFactory = Mock(NamedFactory)
    Build build = Mock(Build)
    File codeBuildDir
    File codeBuildDir0
    File codeBuildDir1
    BuildRunner buildRunner = Mock(BuildRunner)
    Project project = Mock(Project)
    final String projectName = "wiggly"
    final String shortHash = "aaaaaaa"

    void setup() {

        temp = temporaryFolder.getRoot()
        installationInfo = new DefaultInstallationInfo()
        File dataRoot = new File(temp, "kaytee-data")
        System.setProperty(ConstantsKt.baseDir_propertyName, dataRoot.absolutePath)
        step = new DefaultPrepareWorkspace(installationInfo, i18NNamedFactory)

        build.buildRunner >> buildRunner
        buildRunner.project >> project
        project.shortProjectName >> projectName


        build.buildNumber() >> shortHash

        codeBuildDir = new File(temp, "kaytee-data/$projectName/$shortHash")
        codeBuildDir0 = new File(temp, "kaytee-data/$projectName/$shortHash-0")
        codeBuildDir1 = new File(temp, "kaytee-data/$projectName/$shortHash-1")

    }

    def "Execute"() {
        when:
        step.execute(build)

        then:
        codeBuildDir.exists()

        when:
        step.execute(build)

        then:
        codeBuildDir.exists()
        codeBuildDir0.exists()
        !codeBuildDir1.exists()

        when:
        step.execute(build)

        then:
        codeBuildDir.exists()
        codeBuildDir0.exists()
        codeBuildDir1.exists()

    }
}
