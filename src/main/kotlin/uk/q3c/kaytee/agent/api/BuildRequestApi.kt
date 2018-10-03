package uk.q3c.kaytee.agent.api

import uk.q3c.build.gitplus.remote.ServiceProvider
import uk.q3c.rest.hal.HalResource
import java.net.URI
import java.util.*

/**
 * Created by David Sowerby on 10 Mar 2017
 */

/**
 * Created by David Sowerby on 08 Mar 2017
 */

class BuildRequest @JvmOverloads constructor(val projectUri: String, val commitId: String, val serviceProvider: ServiceProvider = serviceProviderFromUri(projectUri)) : HalResource()

class BuildRequestResponse(val projectFullName: String, val commitId: String, val buildId: UUID) : HalResource() {

    constructor(buildRequest: BuildRequest, buildId: UUID) : this(URI(buildRequest.projectUri).path, buildRequest.commitId, buildId)
}


fun serviceProviderFromUri(projectUri: String): ServiceProvider {
    val uri = projectUri.toLowerCase()
    return when {
        uri.contains("gitlab") -> ServiceProvider.GITLAB
        uri.contains("github") -> ServiceProvider.GITHUB
        uri.contains("bitbucket") -> ServiceProvider.BITBUCKET

        else -> {
            ServiceProvider.NOT_SPECIFIED
        }
    }
}