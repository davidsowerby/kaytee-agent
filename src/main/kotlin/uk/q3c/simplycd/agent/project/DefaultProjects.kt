package uk.q3c.simplycd.agent.project

import uk.q3c.simplycd.agent.api.BuildRequestRequest
import uk.q3c.simplycd.agent.app.InvalidPropertyValueException
import uk.q3c.simplycd.project.Project
import java.util.*

/**
 * Created by David Sowerby on 08 Mar 2017
 */
class DefaultProjects : Projects {
    override fun getProject(buildRequestRequest: BuildRequestRequest): Project {
        val projectName = buildRequestRequest.projectFullName
        if (projectName.isBlank() || !(projectName.contains("/"))) {
            throw InvalidPropertyValueException("Project full name must not be blank and must contain a '/'")
        }
        //TODO persistence for projects
        return DefaultProject(buildRequestRequest.projectFullName, UUID.randomUUID())
    }


}