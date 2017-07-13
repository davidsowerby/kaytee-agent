package uk.q3c.kaytee.agent.prepare

import net.engio.mbassy.bus.IMessagePublication
import net.engio.mbassy.bus.MBassador
import spock.lang.Specification
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.LabelKey
import uk.q3c.kaytee.agent.i18n.Named
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.BuildRunner
import uk.q3c.kaytee.agent.queue.PreparationStartedMessage
import uk.q3c.kaytee.agent.queue.PreparationSuccessfulMessage
/**
 * Created by David Sowerby on 19 Jan 2017
 */
class DefaultPreparationStageTest extends Specification {

    DefaultPreparationStage stage
    GitClone gitClone = Mock(GitClone)
    PrepareWorkspace prepareWorkspace = Mock(PrepareWorkspace)
    LoadBuildConfiguration loadBuildConfiguration = Mock(LoadBuildConfiguration)
    Build build = Mock(Build)
    Project project = Mock(Project)
    String userName = 'any'
    NamedFactory namedFactory = Mock(NamedFactory)
    Named named = Mock(Named)
    GlobalBusProvider busProvider = Mock(GlobalBusProvider)
    MBassador<BusMessage> globalBus = Mock(MBassador)
    BuildRunner buildRunner = Mock(BuildRunner)
    ConnectBuildToGradle connectBuildToGradle = Mock(ConnectBuildToGradle)
    UUID uid = UUID.randomUUID()
    IMessagePublication messagePublication = Mock()

    void setup() {
        namedFactory.create(LabelKey.Preparation_Stage) >> named
        named.name() >> "Preparation Stage"
        build.buildRunner >> buildRunner
        buildRunner.identity() >> "whatever"
        buildRunner.uid >> uid
        busProvider.get() >> globalBus
    }

    /**
     * NOTE:  This needs the configuration phase setting up, so that prep takes its setting from the project
     */
    def "steps defined"() {

        given:
        stage = new DefaultPreparationStage(busProvider, gitClone, prepareWorkspace, connectBuildToGradle, loadBuildConfiguration, namedFactory)

        when:
        stage.execute(build)

        then:
        stage.name() == "Preparation Stage"
        1 * globalBus.publishAsync(new PreparationStartedMessage(buildRunner.uid, false)) >> messagePublication

        stage.steps.size() == 4
        stage.steps.get(0) == prepareWorkspace
        stage.steps.get(1) == gitClone
        stage.steps.get(2) == connectBuildToGradle
        stage.steps.get(3) == loadBuildConfiguration

        then:
        prepareWorkspace.execute(build)

        then:
        gitClone.execute(build)

        then:
        loadBuildConfiguration.execute(build)

        then:
        1 * globalBus.publishAsync(new PreparationSuccessfulMessage(buildRunner.uid, false)) >> messagePublication
    }
}
