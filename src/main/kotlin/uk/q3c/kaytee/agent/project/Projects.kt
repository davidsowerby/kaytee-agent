package uk.q3c.kaytee.agent.project

import uk.q3c.kaytee.agent.api.BuildRequest

/**
 * Created by David Sowerby on 08 Mar 2017
 */
interface Projects {
    fun getProject(buildRequestRequest: BuildRequest): Project
    fun getProject(projectFullName: String): Project
    fun getProject(projectUserName: String, projectRepoName: String): Project
}