package uk.q3c.kaytee.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import net.engio.mbassy.listener.References
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.eventbus.GlobalBus
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.eventbus.SubscribeTo
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.project.Projects
import uk.q3c.kaytee.plugin.GroupConfig
import uk.q3c.kaytee.plugin.TaskKey
import java.util.*
import javax.annotation.concurrent.ThreadSafe

/**
 *
 * A TaskRunner implementation for the situation where a task within a project is delegated to another project - for example, where
 * a functional test is developed as a separate project
 *
 * **Note:**  References for the Global Bus are set to strong, as this object may disappear before the closing call to [delegateResult] is made.
 * This is because the RequestQueue does not hold any references to the runner once they have been taken from the queue and executed
 *
 * This class is Thread safe to guard against the remote possibility that an asynchronous message is received via a @Handler method while other processing is underway
 * Created by David Sowerby on 04 Jun 2017
 */
@ThreadSafe
@Listener(references = References.Strong) @SubscribeTo(GlobalBus::class)
class DefaultDelegatedProjectTaskRunner @Inject constructor(
        @Assisted override val build: Build,
        @Assisted override val taskKey: TaskKey,
        @Assisted override val groupConfig: GroupConfig,
        val requestQueue: RequestQueue,
        val projects: Projects,
        val globalBusProvider: GlobalBusProvider)

    : DelegatedProjectTaskRunner {

    private val lock = Any()
    private var delegateBuildId: UUID = UUID.randomUUID()
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun identity(): String {
        return "${build.buildRunner.project.shortProjectName}:${build.buildRunner.uid}:$taskKey}"
    }

    /**
     * Adds a delegate build to the [RequestQueue].  Once executed from the queue, the delegate will send a [TaskSuccessfulMessage] or a [TaskFailedMessage] depending on the outcome
     */
    override fun run() {
        log.info("Starting delegated task {} for parent build {}", taskKey.name, build.buildRunner.uid)
        val globalBus = globalBusProvider.get()
        synchronized(lock) {
            try {

                log.debug("publishing TaskStartedMessage for delegate task {}", taskKey)
                globalBus.publishAsync(TaskStartedMessage(this.build.buildRunner.uid, taskKey, build.buildRunner.delegated))
                val project = projects.getProject(groupConfig.delegate.repoUserName, groupConfig.delegate.repoName)
                val commitSha = GitSHA(groupConfig.delegate.commitId)
                log.debug("build id for delegate is: {}", delegateBuildId)
                delegateBuildId = requestQueue.addRequest(project = project, commitId = commitSha, delegated = true, delegatedTask = groupConfig.delegate.taskToRun)
            } catch(e: Exception) {
                var msg = ""
                if (e.message != null) {
                    msg = e.message as String
                }
                val outcome = TaskFailedMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, TaskStateKey.Failed, msg, "Task for execution by delegate project")
                log.debug("publishing TaskFailedMessage for {}", this)
                globalBus.publishAsync(outcome)
            }
        }
    }

    @Handler
    fun delegateResult(buildMessage: BuildSuccessfulMessage) {
        synchronized(lock) {
            log.trace("Build {} : BuildSuccessfulMessage received from {}", build.buildRunner.uid, buildMessage.buildRequestId)
            if (buildMessage.buildRequestId == delegateBuildId) {
                log.debug("Build {} : processing BuildSuccessfulMessage received from {}", build.buildRunner.uid, buildMessage.buildRequestId)
                val taskMessage = TaskSuccessfulMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, "Delegate build ${buildMessage.buildRequestId} completed successfully")
                publishAndUnsubscribe(taskMessage)
            } else {
                log.trace("Build {} : ignored BuildSuccessfulMessage received from {}, not relevant to this runner", build.buildRunner.uid, buildMessage.buildRequestId)
            }
        }
    }

    @Handler
    fun delegateResult(buildMessage: BuildFailedMessage) {
        synchronized(lock) {
            log.trace("Build {} : BuildFailedMessage received from {}", build.buildRunner.uid, buildMessage.buildRequestId)
            if (buildMessage.buildRequestId == delegateBuildId) {
                log.debug("Build {} : processing BuildFailedMessage received from {}", build.buildRunner.uid, buildMessage.buildRequestId)
                val taskMessage = TaskFailedMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, TaskStateKey.Failed, "${buildMessage.e.message}", "Delegate build ${buildMessage.buildRequestId} failed")
                publishAndUnsubscribe(taskMessage)
            } else {
                log.trace("Build {} : ignored BuildFailedMessage received from {}, not relevant to this runner", build.buildRunner.uid, buildMessage.buildRequestId)
            }
        }
    }

    @Handler
    fun delegateResult(buildMessage: PreparationFailedMessage) {
        synchronized(lock) {
            log.trace("Build {} : PreparationFailedMessage received from {}", build.buildRunner.uid, buildMessage.buildRequestId)
            if (buildMessage.buildRequestId == delegateBuildId) {
                log.debug("Build {} : processing PreparationFailedMessage received from {}", build.buildRunner.uid, buildMessage.buildRequestId)
                val taskMessage = TaskFailedMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, TaskStateKey.Failed, "${buildMessage.e.message}", "Delegate build ${buildMessage.buildRequestId} failed")
                publishAndUnsubscribe(taskMessage)
            } else {
                log.trace("Build {} : ignored PreparationFailedMessage received from {}, not relevant to this runner", build.buildRunner.uid, buildMessage.buildRequestId)
            }
        }
    }

    private fun publishAndUnsubscribe(taskMessage: BusMessage) {
        val globalBus = globalBusProvider.get()
        log.debug("publishing {} for {}", taskMessage.javaClass.simpleName, this)
        globalBus.publishAsync(taskMessage)
        globalBus.unsubscribe(this)
    }

    override fun toString(): String {
        return "Delegate Runner for $taskKey delegate build: $delegateBuildId, parent build: ${build.buildRunner.uid}"
    }
}