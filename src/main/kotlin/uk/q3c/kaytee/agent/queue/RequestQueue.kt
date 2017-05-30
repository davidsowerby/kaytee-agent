package uk.q3c.kaytee.agent.queue

import org.gradle.tooling.CancellationTokenSource
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.project.Project
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A common queue used in a provider-consumer fashion to process build and task requests
 *
 * Created by David Sowerby on 07 Jan 2017
 */
interface RequestQueue {
    /**
     * Gradle uses instances of [CancellationTokenSource] to stop a running build.  When the connection is made to Gradle,
     * a stopper is added so that a request to stopo the job can be executed from the queue
     */
    val stoppers: ConcurrentHashMap<BuildRunner, CancellationTokenSource>

    /**
     * Add a queueRequest to the queue.  Requests may be for a full build of all the steps which are enabled within the
     * 'kaytee' configuration of the project's build.gradle file, or they are individual tasks within a build.
     * The original [BuildRunner] is broken down into a sequence of [TaskRunner]s, which are placed onto the queue
     *
     * @param project the project to build
     * @param gitSHA the Git commit id to build
     *
     * @return the UUID for the request - this is a permanent id for the build request, which also then becomes the Id for build itself
     */
    fun addRequest(project: Project, gitSHA: GitSHA): UUID

    /**
     * Add a [TaskRunner] to the queue
     */
    fun addRequest(taskRunner: TaskRunner)

    /**
     * Returns the number of requests currently in the queue
     *
     * @return the number of requests currently in the queue
     */
    fun size(): Int

    /**
     * The [queueRequest] is removed from the queue.  Ignored if the queueRequest is not in the queue
     *
     * @return true if the queueRequest was successfully removed, false if not found
     */
    fun removeRequest(queueRequest: QueueRequest): Boolean


    fun contains(queueRequest: QueueRequest): Boolean

    /**
     * Attempts to stop a currently executing [BuildRunner].  Uses a Gradle [CancellationTokenSource] to stop a Gradle
     * build.  Ignored if the request is not being built at the time
     */
    fun stopBuild(buildRunner: BuildRunner)

}
