package uk.q3c.kaytee.agent.queue

import com.google.inject.Inject
import com.google.inject.Singleton
import net.engio.mbassy.listener.Listener
import org.gradle.tooling.CancellationTokenSource
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.build.BuildRunnerFactory
import uk.q3c.kaytee.agent.eventbus.GlobalBus
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.eventbus.SubscribeTo
import uk.q3c.kaytee.agent.i18n.MessageKey
import uk.q3c.kaytee.agent.i18n.MessageKey.*
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.system.RestNotifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 *
 * The main queue could have been injected directly by Guice, but the [stopBuild] and [removeRequest] methods need
 * logic beyond put and get - in effect this implementation becomes more of a queue manager
 *
 * Created by David Sowerby on 07 Jan 2017
 */
@Singleton @Listener @SubscribeTo(GlobalBus::class)
class DefaultRequestQueue @Inject constructor(
        val restNotifier: RestNotifier,
        val buildRunnerFactory: BuildRunnerFactory,
        val globalBusProvider: GlobalBusProvider)


    : RequestQueue {


    private val log = LoggerFactory.getLogger(this.javaClass.name)
    private val executor = ThreadPoolExecutor(5, 5, 1, TimeUnit.MINUTES, LinkedBlockingQueue<Runnable>())
    override val stoppers: ConcurrentHashMap<BuildRunner, CancellationTokenSource> = ConcurrentHashMap()
    private val idLock = Any()

    init {
        Thread.setDefaultUncaughtExceptionHandler(ThreadExceptionHandler())
    }

    override fun addRequest(project: Project, commitId: GitSHA): UUID {
        return addRequest(project, commitId, false, "")
    }

    override fun addRequest(project: Project, commitId: GitSHA, delegated: Boolean, delegatedTask: String): UUID {
        synchronized(idLock) {
            val uid = UUID.randomUUID()
            val buildRunner = buildRunnerFactory.create(project, commitId, uid, delegated, delegatedTask)
            executor.submit(buildRunner)
            globalBusProvider.get().publishAsync(BuildQueuedMessage(buildRunner.uid, delegated))
            log.info("Sent request to queue build $uid (Project '{}').  Current (transient) queue size is: {}", buildRunner.project.remoteUri.path, this.size())
            return uid
        }
    }


    override fun addRequest(taskRunner: TaskRunner) {
        log.debug("publishing TaskRequestedMessage for {}", taskRunner.taskKey.name)
        globalBusProvider.get().publishAsync(TaskRequestedMessage(taskRunner.build.buildRunner.uid, taskRunner.taskKey, taskRunner.build.buildRunner.delegated))
        executor.submit(taskRunner)
    }

    //TODo there is a very small possibility that a build hasn't actually started when this is received even though
    // the stopper has been registered.  Best way to deal with that?
    override fun stopBuild(buildRunner: BuildRunner) {
        synchronized(stoppers) {
            val stopper: CancellationTokenSource? = stoppers.get(buildRunner)
            if (stopper == null) {
                userNotifyInfo(Build_Request_Stop_Not_Found, buildRunner)
            } else {
                stopper.cancel()
                userNotifyInfo(Build_Request_Stop_Sent, buildRunner)
            }
        }
    }


    override fun removeRequest(queueRequest: QueueRequest): Boolean {
        synchronized(executor) {
            log.debug("removing queueRequest from queue: '{}'", queueRequest)
            val found = executor.remove(queueRequest)

            // it has been removed from queue, just let the user know
            if (found) {
                userNotifyInfo(Build_Request_Removed_from_Queue, queueRequest)
                return true
            }

            // could not find it - out of date queueRequest
            userNotifyInfo(Build_Request_Remove_Not_Found, queueRequest)
            return false
        }
    }

    private fun userNotifyInfo(key: MessageKey, queueRequest: QueueRequest) {
        restNotifier.notifyInformation(key, queueRequest.identity())
    }


    override fun size(): Int {
        return executor.queue.size
    }


    override fun contains(queueRequest: QueueRequest): Boolean {
        return executor.queue.contains(queueRequest)
    }


}

class ThreadExceptionHandler : Thread.UncaughtExceptionHandler {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>> exception handler called")
//        log.error("Exception in thread", e)
//        throw RuntimeException("Exception in thread", e)
    }

}