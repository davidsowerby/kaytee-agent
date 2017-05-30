package uk.q3c.kaytee.agent.prepare

import net.engio.mbassy.bus.common.PubSubSupport
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.build.BuildFactory
import uk.q3c.kaytee.agent.build.BuildNumberReader
import uk.q3c.kaytee.agent.build.DefaultBuild
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.Named
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.krail.core.i18n.Translate

/**
 * Created by David Sowerby on 20 Jan 2017
 */
abstract class PreparationStepSpecification extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp
    File codeDir

    NamedFactory i18NNamedFactory = Mock(NamedFactory)
    InstallationInfo installationInfo = Mock(InstallationInfo)
    Translate translate = Mock(Translate)
    String translatedKey = 'translated key'
    Build build
    BuildNumberReader buildNumberReader = Mock(BuildNumberReader)
    GitSHA gitHash
    Project project = Mock(Project)
    String projectName = 'wiggly'
    String repoUserName = 'davidsowerby'
    Named i18NNamed = Mock(Named)
    BuildFactory buildFactory = Mock(BuildFactory)
    GlobalBusProvider busProvider = Mock(GlobalBusProvider)
    PreparationStage preparationStage = Mock(PreparationStage)
    RequestQueue requestQueue = Mock(RequestQueue)
    GradleTaskRunnerFactory gradleTaskRequestFactory = Mock(GradleTaskRunnerFactory)
    ManualTaskRunnerFactory manualTaskRequestFactory = Mock(ManualTaskRunnerFactory)
    PubSubSupport<BusMessage> globalBus = Mock(PubSubSupport)

    def setup() {
        gitHash = new GitSHA(DigestUtils.sha1Hex('any'))
        project.shortProjectName >> projectName
        project.remoteUserName >> repoUserName
        BuildRunner buildRequest = new DefaultBuildRunner(buildFactory, busProvider, gitHash, project, UUID.randomUUID())
        build = new DefaultBuild(preparationStage, buildNumberReader, requestQueue, busProvider, gradleTaskRequestFactory, manualTaskRequestFactory, buildRequest)
        translate.from(_) >> translatedKey
        temp = temporaryFolder.getRoot()
        codeDir = new File(temp, projectName)
        i18NNamedFactory.create(_) >> i18NNamed
        installationInfo.projectInstanceDir(build) >> codeDir
        installationInfo.buildNumberDir(build) >> codeDir
        busProvider.get() >> globalBus
    }


}
