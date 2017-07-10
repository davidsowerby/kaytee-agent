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

/**
 *
 * A TaskRunner implementation for the situation where a task within a project is delegated to another project - for example, where
 * a functional test is developed as a separate project
 *
 * **Note:**  References for the Global Bus are set to strong, as this object may disappear before the closing call to [delegateResult] is made.
 * This is because the RequestQueue does not hold any references to the runner once they have been taken from the queue and executed
 *
 * Created by David Sowerby on 04 Jun 2017
 */
@Listener(references = References.Strong) @SubscribeTo(GlobalBus::class)
class DefaultDelegatedProjectTaskRunner @Inject constructor(
        @Assisted override val build: Build,
        @Assisted override val taskKey: TaskKey,
        @Assisted override val groupConfig: GroupConfig,
        val requestQueue: RequestQueue,
        val projects: Projects,
        val globalBusProvider: GlobalBusProvider)

    : DelegatedProjectTaskRunner {

    private var delegateBuildId: UUID = UUID.randomUUID()
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun identity(): String {
        return "${build.buildRunner.project.shortProjectName}:${build.buildRunner.uid}:$taskKey}"
    }

    /**
     * Adds a delegate build to the [RequestQueue].  Once executed from the queue, the delegate will send a [TaskSuccessfulMessage] or a [TaskFailedMessage] depending on the outcome
     */
    override fun run() {
        log.info("Starting delegated task {} for build {}", taskKey.name, build.buildRunner.uid)
        val globalBus = globalBusProvider.get()
        try {
            log.debug("publishing TaskStartedMessage for {}", this)
            globalBus.publish(TaskStartedMessage(this.build.buildRunner.uid, taskKey, build.buildRunner.delegated))
            val project = projects.getProject(groupConfig.delegate.repoUserName, groupConfig.delegate.repoName)
            delegateBuildId = requestQueue.addRequest(project = project, commitId = GitSHA(groupConfig.delegate.commitId), delegated = true, delegatedTask = groupConfig.delegate.taskToRun)
        } catch(e: Exception) {
            var msg = ""
            if (e.message != null) {
                msg = e.message as String
            }
            val outcome = TaskFailedMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, TaskStateKey.Failed, msg, "Task for execution by delegate project")
            log.debug("publishing TaskFailedMessage for {}", this)
            globalBus.publish(outcome)
        }
    }

    @Handler
    fun delegateResult(buildMessage: BuildSuccessfulMessage) {
        log.debug("Delegate successful message received by {}", this)
        if (buildMessage.buildRequestId == delegateBuildId) {
            val taskMessage = TaskSuccessfulMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, "Delegate build ${buildMessage.buildRequestId} completed successfully")
            publishAndUnsubscribe(taskMessage)
        } else {
            log.debug("{} message ignored, not relevant to this build {}", buildMessage.javaClass.simpleName, this)
        }
    }

    private fun publishAndUnsubscribe(taskMessage: BusMessage) {
        val globalBus = globalBusProvider.get()
        log.debug("publishing {} for {}", taskMessage.javaClass.simpleName, this)
        globalBus.publish(taskMessage)
        globalBus.unsubscribe(this)
    }

    @Handler
    fun delegateResult(buildMessage: BuildFailedMessage) {
        if (buildMessage.buildRequestId == delegateBuildId) {
            log.debug("Delegate failed message received by {}", this)
            val taskMessage = TaskFailedMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, TaskStateKey.Failed, "${buildMessage.e.message}", "Delegate build ${buildMessage.buildRequestId} failed")
            publishAndUnsubscribe(taskMessage)
        } else {
            log.debug("{} message ignored, not relevant to this build {}", buildMessage.javaClass.simpleName, this)
        }
    }

    override fun toString(): String {
        return "Delegate Runner for ${build}, ${taskKey}"
    }
}