package uk.q3c.simplycd.lifecycle.prepare

import uk.q3c.simplycd.build.Build
import uk.q3c.simplycd.i18n.Named

/**
 * Created by David Sowerby on 21 Jan 2017
 */
interface PreparationBuildStep : Named {

    fun execute(build: Build)
}