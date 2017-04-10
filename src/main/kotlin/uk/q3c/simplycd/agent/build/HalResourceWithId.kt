package uk.q3c.simplycd.agent.build

import uk.q3c.rest.hal.HalResource
import java.util.*

/**
 * Created by David Sowerby on 09 Apr 2017
 */
open class HalResourceWithId(val uid: UUID, val path: String) : HalResource() {

    init {
        self("${path}/?uid=$uid")
    }
}