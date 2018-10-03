package uk.q3c.kaytee.agent.project

import uk.q3c.build.gitplus.remote.ServiceProvider
import uk.q3c.kaytee.agent.api.BuildRequest
import java.net.URI

/**
 * Created by David Sowerby on 08 Mar 2017
 */
interface Projects {
    fun getProject(buildRequestRequest: BuildRequest): Project
    fun getProject(serviceProvider: ServiceProvider, projectFqPath: String): Project
    fun getProject(serviceProvider: ServiceProvider, projectNamespace: String, projectRepoName: String): Project
    fun getProject(provider: ServiceProvider, projectUri: URI): Project
    fun getProject(projectUri: URI): Project
}