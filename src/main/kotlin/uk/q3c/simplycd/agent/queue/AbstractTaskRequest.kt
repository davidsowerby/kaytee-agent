package uk.q3c.simplycd.queue

import net.engio.mbassy.bus.common.PubSubSupport
import org.slf4j.LoggerFactory
import uk.q3c.krail.core.eventbus.BusMessage
import uk.q3c.simplycd.build.Build
import uk.q3c.simplycd.i18n.TaskKey
import java.time.LocalDateTime

/**
 * Created by David Sowerby on 26 Jan 2017
 */
abstract class AbstractTaskRequest constructor(
        override val build: Build,
        override val taskKey: TaskKey,
        val globalBus: PubSubSupport<BusMessage>) :

        TaskRequest {

    private val log = LoggerFactory.getLogger(this.javaClass.name)


    override fun run() {
        log.info("Executing task request {}", identity())
        val start = LocalDateTime.now()
        globalBus.publish(TaskStartedMessage(start = start, taskRequest = this))
        doRun()
        // we cannot send the end message here - some tasks are executed asynchronously
    }

    override fun identity(): String {
        return "${build.buildRequest.project.name}:${build.buildNumber()}:$taskKey}"
    }

    override fun toString(): String {
        return identity()
    }

    abstract fun doRun()


}