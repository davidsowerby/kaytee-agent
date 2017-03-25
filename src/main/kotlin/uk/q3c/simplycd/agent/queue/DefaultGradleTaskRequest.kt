package uk.q3c.simplycd.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.build.BuildExceptionLookup
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.i18n.BuildResultStateKey
import uk.q3c.simplycd.i18n.TaskKey
import java.time.LocalDateTime


/**
 *
 * Represents a single 'task' call to Gradle (though that may contain multiple Gradle tasks, for example 'clean test')
 *
 * Created by David Sowerby on 26 Jan 2017
 */
class DefaultGradleTaskRequest @Inject constructor(globalBusProvider: GlobalBusProvider, val gradleTaskExecutor: GradleTaskExecutor, @Assisted build: Build, @Assisted taskKey: TaskKey) :

        GradleTaskRequest,
        AbstractTaskRequest(build, taskKey, globalBusProvider.get()) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun doRun() {
        //split the task line on whitespace for the call to build.tasks
        val start = LocalDateTime.now()


        try {
            gradleTaskExecutor.execute(build, taskKey)
            log.info("Build successful for {}", identity())
            val result = TaskCompletedMessage(start = start, end = LocalDateTime.now(), result = BuildResultStateKey.Build_Successful, taskRequest = this)
            globalBus.publish(result)
        } catch (e: Exception) {
            log.info("Build failed for {}", identity())
            val result = TaskFailedMessage(start = start, end = LocalDateTime.now(), result = BuildExceptionLookup().lookupKeyFromException(e), taskRequest = this, exception = e)
            globalBus.publish(result)
        }
    }

    override fun toString(): String {
        return identity()
    }


}