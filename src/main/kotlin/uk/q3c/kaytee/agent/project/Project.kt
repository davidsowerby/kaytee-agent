package uk.q3c.kaytee.agent.project

import uk.q3c.build.gitplus.remote.ServiceProvider
import java.net.URI
import java.util.*

/**
 * Created by David Sowerby on 07 Jan 2017
 */
interface Project {
    val remoteProvider: ServiceProvider
    val remoteUri: URI
    val uid: UUID
    val projectName: String
    val remoteNamespace: String
    val fqProjectName: String
}