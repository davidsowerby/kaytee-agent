package uk.q3c.simplycd.agent.queue

import net.engio.mbassy.bus.common.PubSubSupport
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.eventbus.BusMessage
import uk.q3c.simplycd.agent.i18n.TaskKey
import uk.q3c.simplycd.agent.i18n.TaskResultStateKey

/**
 * Created by David Sowerby on 26 Jan 2017
 */
abstract class AbstractTaskRunner constructor(
        override val build: Build,
        override val taskKey: TaskKey,
        val globalBus: PubSubSupport<BusMessage>) :

        TaskRunner {

    private val log = LoggerFactory.getLogger(this.javaClass.name)


    override fun run() {
        try {
            globalBus.publish(TaskStartedMessage(this.build.buildRunner.uid, taskKey))
            log.info("Executing task request {}", identity())
            doRun()
        } catch (e: Exception) {
            globalBus.publish(TaskFailedMessage(this.build.buildRunner.uid, taskKey, TaskResultStateKey.Task_Failed))
            log.error("Exception thrown by task execution", e)
        }
        // we cannot send the end message here - some tasks are executed asynchronously
    }

    override fun identity(): String {
        return "${build.buildRunner.project.shortProjectName}:${build.buildNumber()}:$taskKey}"
    }

    override fun toString(): String {
        return identity()
    }

    abstract fun doRun()


}