package uk.q3c.simplycd.agent.prepare

import net.engio.mbassy.bus.common.PubSubSupport
import spock.lang.Specification
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.eventbus.BusMessage
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.agent.i18n.LabelKey
import uk.q3c.simplycd.agent.i18n.NamedFactory
import uk.q3c.simplycd.agent.project.Project
import uk.q3c.simplycd.agent.queue.BuildRequest
import uk.q3c.simplycd.agent.queue.PreparationStartedMessage
import uk.q3c.simplycd.agent.queue.PreparationSuccessfulMessage
import uk.q3c.simplycd.i18n.Named

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
    PubSubSupport<BusMessage> globalBus = Mock(PubSubSupport)
    BuildRequest buildRequest = Mock(BuildRequest)
    ConnectBuildToGradle connectBuildToGradle = Mock(ConnectBuildToGradle)
    UUID uid = UUID.randomUUID()

    void setup() {
        namedFactory.create(LabelKey.Preparation_Stage) >> named
        named.name() >> "Preparation Stage"
        build.buildRequest >> buildRequest
        buildRequest.identity() >> "whatever"
        buildRequest.uid >> uid
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
        1 * globalBus.publish(new PreparationStartedMessage(buildRequest.uid))

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
        1 * globalBus.publish(new PreparationSuccessfulMessage(buildRequest.uid))

    }
}
