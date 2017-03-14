package uk.q3c.simplycd.agent.app

import uk.q3c.rest.hal.HalResource
import java.util.*

/**
 * Created by David Sowerby on 13 Mar 2017
 */
class BuildStatusMessage(val buildId: UUID) : HalResource() {
    init {
        self(href("build/$buildId"))
    }
}