package uk.q3c.kaytee.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.build.BuildFactory
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.project.Project
import java.util.*

/**
 *  Used to place a request in [RequestQueue]
 *
 * Created by David Sowerby on 08 Jan 2017
 */
data class DefaultBuildRunner @Inject constructor(val buildFactory: BuildFactory,
                                                  val globalBusProvider: GlobalBusProvider,
                                                  @Assisted override val delegated: Boolean,
                                                  @Assisted override val delegateTask: String,
                                                  @Assisted override val gitHash: GitSHA,
                                                  @Assisted override val project: Project,
                                                  @Assisted override val uid: UUID) : BuildRunner {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun run() {
        try {
            log.debug("creating build instance")
            val build = buildFactory.create(this, delegated)
            log.debug("executing build instance, build Id: {}", build.buildRunner.uid)
            build.execute()
        } catch (e: Exception) {
            globalBusProvider.get().publishAsync(BuildFailedMessage(uid, delegated, e))
            log.error("Exception thrown in build execution", e)
        }
    }

    override fun identity(): String {
        return "project: '${project.remoteUri.path}' git sha:${gitHash.sha}"
    }

    override fun toString(): String {
        return identity()
    }
}