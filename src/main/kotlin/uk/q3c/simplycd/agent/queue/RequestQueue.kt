package uk.q3c.simplycd.queue

import org.gradle.tooling.CancellationTokenSource
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.simplycd.project.Project
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
    val stoppers: ConcurrentHashMap<BuildRequest, CancellationTokenSource>

    /**
     * Add a queueRequest to the queue.  Requests may be for a full build of all the steps which are enabled within the
     * 'simplycd' configuration of the project's build.gradle file, or they are individual tasks within a build.
     * The original [BuildRequest] is broken down into a sequence of [TaskRequest]s, which are placed onto the queue
     *
     * @param project the project to build
     * @param gitSHA the Git commit id to build
     */
    fun addRequest(project: Project, gitSHA: GitSHA)

    /**
     * Add a [TaskRequest] to the queue
     */
    fun addRequest(taskRequest: TaskRequest)

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
     * Attempts to stop a currently executing [BuildRequest].  Uses a Gradle [CancellationTokenSource] to stop a Gradle
     * build.  Ignored if the request is not being built at the time
     */
    fun stopBuild(buildRequest: BuildRequest)

}
