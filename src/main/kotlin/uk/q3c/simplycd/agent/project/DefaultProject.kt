package uk.q3c.simplycd.agent.project

import com.google.inject.Inject
import java.util.*

/**
 * Created by David Sowerby on 16 Jan 2017
 */
data class DefaultProject @Inject constructor(override val fullProjectName: String, override val uid: UUID) : Project {

    override val shortProjectName: String = fullProjectName.split("/")[1]
    override val remoteUserName: String = fullProjectName.split("/")[0]


}