package uk.q3c.simplycd.agent.api

import uk.q3c.rest.hal.HalResource
import java.util.*

/**
 * Created by David Sowerby on 10 Mar 2017
 */

/**
 * Created by David Sowerby on 08 Mar 2017
 */
class BuildRequest(val projectFullName: String, val commitId: String) : HalResource()

class BuildRequestResponse(val projectFullName: String, val commitId: String, val buildId: UUID) : HalResource() {

    constructor(buildRequest: BuildRequest, buildId: UUID) : this(buildRequest.projectFullName, buildRequest.commitId, buildId)
}