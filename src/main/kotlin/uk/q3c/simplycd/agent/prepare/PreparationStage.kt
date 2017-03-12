package uk.q3c.simplycd.agent.prepare

import com.google.common.collect.ImmutableList
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.i18n.Named

/**
 * Created by David Sowerby on 17 Jan 2017
 */
interface PreparationStage : Named {
    var steps: ImmutableList<PreparationBuildStep>
    fun execute(build: Build)
}