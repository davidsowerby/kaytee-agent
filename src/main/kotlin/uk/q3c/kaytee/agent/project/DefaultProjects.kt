package uk.q3c.kaytee.agent.project

import uk.q3c.build.gitplus.remote.ServiceProvider
import uk.q3c.kaytee.agent.api.BuildRequest
import java.net.URI
import java.util.*

/**
 * Created by David Sowerby on 08 Mar 2017
 */
class DefaultProjects : Projects {
    override fun getProject(serviceProvider: ServiceProvider, projectFqPath: String): Project {
        return getProject(serviceProvider, URI("${baseUrl(serviceProvider)}/$projectFqPath"))
    }

    override fun getProject(serviceProvider: ServiceProvider, projectNamespace: String, projectRepoName: String): Project {
        return getProject(serviceProvider, URI("${baseUrl(serviceProvider)}/$projectNamespace/$projectRepoName"))
    }


    override fun getProject(buildRequestRequest: BuildRequest): Project {
        return DefaultProject(buildRequestRequest.serviceProvider, URI(buildRequestRequest.projectUri), UUID.randomUUID())
    }

    override fun getProject(provider: ServiceProvider, projectUri: URI): Project {
        //TODO persistence for projects
        return DefaultProject(provider, projectUri, UUID.randomUUID())
    }

    override fun getProject(projectUri: URI): Project {
        val uri = projectUri.toString()
        val provider = when {
            uri.contains("gitlab") -> ServiceProvider.GITLAB
            uri.contains("github") -> ServiceProvider.GITHUB
            uri.contains("bitbucket") -> ServiceProvider.BITBUCKET

            else -> {
                ServiceProvider.NOT_SPECIFIED
            }
        }
        return DefaultProject(provider, projectUri, UUID.randomUUID())
    }

    private fun baseUrl(serviceProvider: ServiceProvider): String {
        return when (serviceProvider) {
            ServiceProvider.GITHUB -> "https://github.com"
            ServiceProvider.GITLAB -> "https://gitlab.com"
            ServiceProvider.BITBUCKET -> "https://bitbucket.com"
            ServiceProvider.NOT_SPECIFIED -> throw IllegalArgumentException("service provider must be specified")
        }
    }


}