package uk.q3c.simplycd.agent.project

import uk.q3c.simplycd.agent.api.BuildRequest

/**
 * Created by David Sowerby on 08 Mar 2017
 */
interface Projects {
    fun getProject(buildRequestRequest: BuildRequest): Project
    fun getProject(projectFullName: String): Project
}