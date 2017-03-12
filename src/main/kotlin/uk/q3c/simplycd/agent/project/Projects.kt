package uk.q3c.simplycd.agent.project

import uk.q3c.simplycd.agent.api.BuildRequestRequest
import uk.q3c.simplycd.project.Project

/**
 * Created by David Sowerby on 08 Mar 2017
 */
interface Projects {
    fun getProject(buildRequestRequest: BuildRequestRequest): Project
}