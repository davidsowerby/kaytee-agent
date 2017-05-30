package uk.q3c.kaytee.agent.prepare

import com.google.common.collect.ImmutableList
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.i18n.Named

/**
 * Created by David Sowerby on 17 Jan 2017
 */
interface PreparationStage : Named {
    var steps: ImmutableList<PreparationBuildStep>
    fun execute(build: Build)
}