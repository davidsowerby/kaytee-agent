package uk.q3c.simplycd.agent.prepare

import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.i18n.Named

/**
 * Created by David Sowerby on 21 Jan 2017
 */
interface PreparationBuildStep : Named {

    fun execute(build: Build)
}