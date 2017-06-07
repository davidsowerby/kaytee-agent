package uk.q3c.kaytee.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import uk.q3c.kaytee.agent.system.InstallationInfo


/**
 *
 * Represents a single 'task' call to Gradle (though that may contain multiple Gradle tasks, for example 'clean test')
 *
 * Created by David Sowerby on 26 Jan 2017
 */
class DefaultGradleTaskRunner @Inject constructor(
        val globalBusProvider: GlobalBusProvider,
        val gradleTaskExecutor: GradleTaskExecutor,
        val installationInfo: InstallationInfo,
        @Assisted override val build: Build,
        @Assisted override val taskKey: TaskKey,
        @Assisted val includeQualityGate: Boolean) :

        GradleTaskRunner {

    private val log = LoggerFactory.getLogger(this.javaClass.name)


    override fun run() {
        val globalBus = globalBusProvider.get()
        try {
            globalBus.publish(TaskStartedMessage(this.build.buildRunner.uid, taskKey))
            log.info("Executing task request {}", identity())
            if (build.buildRunner.delegated) {
                gradleTaskExecutor.execute(build, build.buildRunner.delegateTask)
            } else {
                gradleTaskExecutor.execute(build, taskKey, includeQualityGate)
            }
            log.info("Task successful for {}", identity())
            val stdOutFile = installationInfo.gradleStdOutFile(build)
            val outcome = TaskSuccessfulMessage(build.buildRunner.uid, taskKey, stdOutFile.readText()) // any error would cause exception
            globalBus.publish(outcome)
        } catch (e: Exception) {
            val stdErrFile = installationInfo.gradleStdErrFile(build)
            val stdOutFile = installationInfo.gradleStdOutFile(build)
            val errText = stdErrFile.readText()
            log.info("Task failed for {}", identity())
            val resultKey = if (errText.contains("Code coverage failed")) {
                TaskResultStateKey.Quality_Gate_Failed
            } else {
                TaskResultStateKey.Task_Failed
            }
            val outcome = TaskFailedMessage(build.buildRunner.uid, taskKey, resultKey, errText, stdOutFile.readText())
            globalBus.publish(outcome)
        }
        // we cannot send the end message here - some tasks are executed asynchronously
    }

    override fun identity(): String {
        return "${build.buildRunner.project.shortProjectName}:${build.buildRunner.uid}:$taskKey}"
    }

    override fun toString(): String {
        return identity()
    }


}