package uk.q3c.kaytee.agent.project

import com.google.inject.Inject
import uk.q3c.build.gitplus.remote.ServiceProvider
import java.net.URI
import java.util.*

/**
 * Created by David Sowerby on 16 Jan 2017
 */
data class DefaultProject @Inject constructor(override val remoteProvider: ServiceProvider, override val remoteUri: URI, override val uid: UUID) : Project {

    override val projectName: String = remoteUri.path.split("/").last()
    override val remoteNamespace: String = remoteUri.path.replace("/$projectName", "").removePrefix("/")
    override val fqProjectName: String = remoteUri.path.removePrefix("/")


}