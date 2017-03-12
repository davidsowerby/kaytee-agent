package uk.q3c.simplycd.agent.api

import uk.q3c.rest.hal.HalResource

/**
 * Created by David Sowerby on 08 Mar 2017
 */
class BuildRequestRequest(val projectFullName: String, val commitId: String) : HalResource()