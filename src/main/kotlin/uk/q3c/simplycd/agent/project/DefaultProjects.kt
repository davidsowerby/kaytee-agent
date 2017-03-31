package uk.q3c.simplycd.agent.project

import uk.q3c.simplycd.agent.api.BuildRequest
import uk.q3c.simplycd.agent.app.InvalidPropertyValueException
import java.util.*

/**
 * Created by David Sowerby on 08 Mar 2017
 */
class DefaultProjects : Projects {

    override fun getProject(buildRequestRequest: BuildRequest): Project {
        return getProject(buildRequestRequest.projectFullName)
    }

    override fun getProject(projectFullName: String): Project {
        if (projectFullName.isBlank() || !(projectFullName.contains("/"))) {
            throw InvalidPropertyValueException("Project full name must not be blank and must contain a '/'")
        }
        //TODO persistence for projects
        return DefaultProject(projectFullName, UUID.randomUUID())

    }


}