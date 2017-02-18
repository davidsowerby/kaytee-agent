package uk.q3c.simplycd.build

import uk.q3c.simplycd.queue.BuildRequest

/**
 * Created by David Sowerby on 14 Jan 2017
 */
interface BuildFactory {

    fun create(buildRequest: BuildRequest): Build
}