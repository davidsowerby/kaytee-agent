package uk.q3c.simplycd.agent.queue

import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.api.BuildRequestRequest
import java.util.*

/**
 * Created by David Sowerby on 10 Mar 2017
 */

class BuildRequestRequest(val projectFullName: String, val commitId: String) : HalResource()

class BuildRequestResponse(val projectFullName: String, val commitId: String, val buildId: UUID) : HalResource() {

    constructor(buildRequestRequest: BuildRequestRequest, buildId: UUID) : this(buildRequestRequest.projectFullName, buildRequestRequest.commitId, buildId)
}