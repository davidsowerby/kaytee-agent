package uk.q3c.kaytee.agent.queue

import net.engio.mbassy.bus.common.PubSubSupport
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey.Quality_Gate_Failed
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey.Task_Failed
import uk.q3c.kaytee.agent.system.InstallationInfo

/**
 * Created by David Sowerby on 26 Jan 2017
 */
abstract class AbstractTaskRunner constructor(
        override val build: Build,
        override val taskKey: TaskKey,
        val installationInfo: InstallationInfo,
        val globalBus: PubSubSupport<BusMessage>) :

        TaskRunner {

    private val log = LoggerFactory.getLogger(this.javaClass.name)


    override fun run() {
        try {
            globalBus.publish(TaskStartedMessage(this.build.buildRunner.uid, taskKey))
            log.info("Executing task request {}", identity())
            doRun()
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
                Quality_Gate_Failed
            } else {
                Task_Failed
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

    abstract fun doRun()


}