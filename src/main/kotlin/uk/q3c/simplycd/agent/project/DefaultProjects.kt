package uk.q3c.simplycd.agent.project

import uk.q3c.simplycd.agent.api.BuildRequestRequest
import uk.q3c.simplycd.project.Project
import java.util.*

/**
 * Created by David Sowerby on 08 Mar 2017
 */
class DefaultProjects : Projects {
    override fun getProject(buildRequestRequest: BuildRequestRequest): Project {
        //TODO persistence for projects
        return DefaultProject(buildRequestRequest.projectFullName, UUID.randomUUID())
    }


}