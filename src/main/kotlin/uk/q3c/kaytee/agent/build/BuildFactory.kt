package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.queue.BuildRunner


/**
 * Created by David Sowerby on 14 Jan 2017
 */
interface BuildFactory {

    fun create(buildRunner: BuildRunner): Build
}