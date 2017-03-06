package uk.q3c.simplycd.agent.prepare

import net.engio.mbassy.bus.common.PubSubSupport
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.krail.core.eventbus.BusMessage
import uk.q3c.krail.core.eventbus.GlobalBusProvider
import uk.q3c.krail.core.i18n.Translate
import uk.q3c.simplycd.agent.i18n.NamedFactory
import uk.q3c.simplycd.build.Build
import uk.q3c.simplycd.build.BuildFactory
import uk.q3c.simplycd.build.BuildNumberReader
import uk.q3c.simplycd.build.DefaultBuild
import uk.q3c.simplycd.i18n.Named
import uk.q3c.simplycd.lifecycle.prepare.PreparationStage
import uk.q3c.simplycd.project.Project
import uk.q3c.simplycd.queue.*
import uk.q3c.simplycd.system.InstallationInfo

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
    GradleTaskRequestFactory gradleTaskRequestFactory = Mock(GradleTaskRequestFactory)
    ManualTaskRequestFactory manualTaskRequestFactory = Mock(ManualTaskRequestFactory)
    PubSubSupport<BusMessage> globalBus = Mock(PubSubSupport)

    def setup() {
        gitHash = new GitSHA(DigestUtils.sha1Hex('any'))
        project.name >> projectName
        project.remoteUserName >> repoUserName
        BuildRequest buildRequest = new DefaultBuildRequest(buildFactory, gitHash, project)
        build = new DefaultBuild(preparationStage, buildNumberReader, requestQueue, busProvider, gradleTaskRequestFactory, manualTaskRequestFactory, buildRequest)
        translate.from(_) >> translatedKey
        temp = temporaryFolder.getRoot()
        codeDir = new File(temp, projectName)
        i18NNamedFactory.create(_) >> i18NNamed
        installationInfo.codeDir(build) >> codeDir
        busProvider.get() >> globalBus
    }


}
