package uk.q3c.kaytee.agent.prepare

import com.google.inject.Provider
import net.engio.mbassy.bus.MBassador
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.build.gitplus.remote.ServiceProvider
import uk.q3c.kaytee.agent.build.*
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.Named
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.project.DefaultProject
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.krail.i18n.Translate
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
    Project project
    String projectName = 'wiggly'
    String repoNamespace = 'davidsowerby'
    Named i18NNamed = Mock(Named)
    BuildFactory buildFactory = Mock(BuildFactory)
    GlobalBusProvider busProvider = Mock(GlobalBusProvider)
    PreparationStage preparationStage = Mock(PreparationStage)
    RequestQueue requestQueue = Mock(RequestQueue)
    GradleTaskRunnerFactory gradleTaskRunnerFactory = Mock(GradleTaskRunnerFactory)
    ManualTaskRunnerFactory manualTaskRunnerFactory = Mock(ManualTaskRunnerFactory)
    MBassador<BusMessage> globalBus = Mock(MBassador)
    DelegatedProjectTaskRunnerFactory delegatedProjectTaskRunnerFactory = Mock(DelegatedProjectTaskRunnerFactory)
    Provider<IssueCreator> issueCreatorProvider = Mock(Provider)
    IssueCreator issueCreator = Mock(IssueCreator)
    BuildOutputWriter buildRecordWriter = Mock()

    def setup() {
        issueCreatorProvider.get() >> issueCreator
        gitHash = new GitSHA(DigestUtils.sha1Hex('any'))
        project = new DefaultProject(ServiceProvider.GITHUB, new URI("https://github.com/davidsowerby/wiggly"), UUID.randomUUID())
        BuildRunner buildRunner = new DefaultBuildRunner(buildFactory, busProvider, false, "", gitHash, project, UUID.randomUUID())
        build = new DefaultBuild(preparationStage, buildNumberReader, requestQueue, busProvider, gradleTaskRunnerFactory, manualTaskRunnerFactory, delegatedProjectTaskRunnerFactory, issueCreatorProvider, buildRecordWriter, buildRunner)
        translate.from(_) >> translatedKey
        temp = temporaryFolder.getRoot()
        codeDir = new File(temp, projectName)
        i18NNamedFactory.create(_) >> i18NNamed
        installationInfo.projectInstanceDir(build) >> codeDir
        installationInfo.buildNumberDir(build) >> codeDir
        busProvider.get() >> globalBus

    }


}
