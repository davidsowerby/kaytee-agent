package uk.q3c.simplycd.agent.project

import com.google.inject.Inject
import uk.q3c.simplycd.project.Project

/**
 * Created by David Sowerby on 16 Jan 2017
 */
data class DefaultProject @Inject constructor(override val remoteUserName: String, override val name: String) : Project