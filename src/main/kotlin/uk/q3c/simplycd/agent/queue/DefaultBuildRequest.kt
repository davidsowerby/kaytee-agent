package uk.q3c.simplycd.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.simplycd.agent.build.BuildFactory
import uk.q3c.simplycd.project.Project
import java.util.*

/**
 *  Used to place a request in [RequestQueue]
 *
 * Created by David Sowerby on 08 Jan 2017
 */
data class DefaultBuildRequest @Inject constructor(val buildFactory: BuildFactory,
                                                   @Assisted override val gitHash: GitSHA,
                                                   @Assisted override val project: Project,
                                                   @Assisted override val uid: UUID) : BuildRequest {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun run() {
        log.debug("creating build instance")
        val build = buildFactory.create(this)
        log.debug("executing build instance")
        build.execute()
    }

    override fun identity(): String {
        return "${project.shortProjectName}:${gitHash.sha}"
    }

    override fun toString(): String {
        return identity()
    }
}