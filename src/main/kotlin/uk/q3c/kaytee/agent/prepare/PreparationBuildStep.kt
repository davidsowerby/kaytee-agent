package uk.q3c.kaytee.agent.prepare

import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.i18n.Named

/**
 * Created by David Sowerby on 21 Jan 2017
 */
interface PreparationBuildStep : Named {

    fun execute(build: Build)
}