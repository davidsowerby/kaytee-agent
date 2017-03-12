package uk.q3c.simplycd.agent.prepare

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.agent.i18n.LabelKey
import uk.q3c.simplycd.agent.i18n.NamedFactory
import uk.q3c.simplycd.agent.queue.PreparationCompletedMessage
import uk.q3c.simplycd.agent.queue.PreparationStartedMessage
import uk.q3c.simplycd.i18n.Named

/**
 * Created by David Sowerby on 17 Jan 2017
 */
class DefaultPreparationStage @Inject constructor(
        val globalBusProvider: GlobalBusProvider,
        gitClone: GitClone,
        prepareWorkspace: PrepareWorkspace,
        loadBuildConfiguration: LoadBuildConfiguration,
        namedFactory: NamedFactory)

    : PreparationStage, Named by namedFactory.create(LabelKey.Preparation_Stage) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override var steps: ImmutableList<PreparationBuildStep> = ImmutableList.of(prepareWorkspace, gitClone, loadBuildConfiguration)


    override fun execute(build: Build) {
        log.info("Started preparation for build: {}", build.buildRequest.identity())
        globalBusProvider.get().publish(PreparationStartedMessage(build.buildRequest))

        for (step in steps) {
            step.execute(build)
        }
        // build has now been configured and at least one task placed in the queue
        globalBusProvider.get().publish(PreparationCompletedMessage(build.buildRequest))
        log.info("Completed preparation for build:  {}", build.buildRequest.identity())
    }
}