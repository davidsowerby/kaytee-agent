package uk.q3c.kaytee.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import uk.q3c.kaytee.agent.project.Projects
import uk.q3c.kaytee.plugin.GroupConfig

/**
 * Created by David Sowerby on 04 Jun 2017
 */
class DefaultDelegatedProjectTaskRunner @Inject constructor(
        @Assisted override val build: Build,
        @Assisted override val taskKey: TaskKey,
        @Assisted override val groupConfig: GroupConfig,
        val requestQueue: RequestQueue,
        val projects: Projects,
        val globalBusProvider: GlobalBusProvider)

    : DelegatedProjectTaskRunner {

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
            globalBus.publish(TaskStartedMessage(this.build.buildRunner.uid, taskKey))
            val project = projects.getProject(groupConfig.delegate.repoUserName, groupConfig.delegate.repoName)
            requestQueue.addRequest(project = project, commitId = GitSHA(groupConfig.delegate.commitId), delegated = true, delegatedTask = groupConfig.delegate.taskToRun)
        } catch(e: Exception) {
            var msg = ""
            if (e.message != null) {
                msg = e.message as String
            }
            val outcome = TaskFailedMessage(build.buildRunner.uid, taskKey, TaskResultStateKey.Task_Failed, msg, "Task for execution by delegate project")
            globalBus.publish(outcome)
        }
    }
}