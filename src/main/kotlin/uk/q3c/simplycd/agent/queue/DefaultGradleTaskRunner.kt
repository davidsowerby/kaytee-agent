package uk.q3c.simplycd.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.agent.i18n.TaskKey
import uk.q3c.simplycd.agent.i18n.TaskResultStateKey


/**
 *
 * Represents a single 'task' call to Gradle (though that may contain multiple Gradle tasks, for example 'clean test')
 *
 * Created by David Sowerby on 26 Jan 2017
 */
class DefaultGradleTaskRunner @Inject constructor(globalBusProvider: GlobalBusProvider, val gradleTaskExecutor: GradleTaskExecutor, @Assisted build: Build, @Assisted taskKey: TaskKey) :

        GradleTaskRunner,
        AbstractTaskRunner(build, taskKey, globalBusProvider.get()) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun doRun() {
        try {
            gradleTaskExecutor.execute(build, taskKey)
            log.info("Build successful for {}", identity())
            val outcome = TaskSuccessfulMessage(build.buildRunner.uid, taskKey)
            globalBus.publish(outcome)
        } catch (e: Exception) {
            log.info("Build failed for {}", identity())
            val outcome = TaskFailedMessage(build.buildRunner.uid, taskKey, TaskResultStateKey.Task_Failed)
            globalBus.publish(outcome)
        }
    }

    override fun toString(): String {
        return identity()
    }


}