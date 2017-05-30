package uk.q3c.kaytee.agent.project

import java.util.*

/**
 * Created by David Sowerby on 07 Jan 2017
 */
interface Project {
    val uid: UUID
    val fullProjectName: String
    val remoteUserName: String
    val shortProjectName: String
}