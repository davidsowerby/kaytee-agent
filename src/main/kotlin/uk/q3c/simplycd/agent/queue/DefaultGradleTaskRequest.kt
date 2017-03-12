package uk.q3c.simplycd.agent.queue

import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.build.BuildExceptionLookup
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.i18n.BuildResultStateKey
import uk.q3c.simplycd.i18n.TaskKey
import java.io.FileOutputStream
import java.time.LocalDateTime


/**
 *
 * Represents a single 'task' call to Gradle (though that may contain multiple Gradle tasks, for example 'clean test')
 *
 * Created by David Sowerby on 26 Jan 2017
 */
class DefaultGradleTaskRequest @Inject constructor(globalBusProvider: GlobalBusProvider, @Assisted build: Build, @Assisted taskKey: TaskKey) :

        GradleTaskRequest,
        AbstractTaskRequest(build, taskKey, globalBusProvider.get()) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun doRun() {
        //split the task line on whitespace for the call to build.tasks
        val start = LocalDateTime.now()
        val tasks = Splitter.on(CharMatcher.WHITESPACE)
                .omitEmptyStrings()
                .split(taskKey.command())

        try {
            if (!build.stderrOutputFile.exists()) {
                build.stderrOutputFile.createNewFile()
            }
            if (!build.stdoutOutputFile.exists()) {
                build.stdoutOutputFile.createNewFile()
            }
            FileOutputStream(build.stderrOutputFile).use { captureStderr ->
                FileOutputStream(build.stdoutOutputFile).use { captureStdOut ->
                    build.gradleLauncher.forTasks(*Iterables.toArray(tasks, String::class.java))
                            .setStandardOutput(captureStdOut)
                            .setStandardError(captureStderr)
                    log.info("Executing Gradle task request for {}, with Gradle command: '{}'", taskKey, taskKey.command())
                    build.gradleLauncher.run()
                }
            }
            log.info("Build successful for {}", identity())
            val result = TaskCompletedMessage(start = start, end = LocalDateTime.now(), result = BuildResultStateKey.Build_Successful, taskRequest = this)
            globalBus.publish(result)
        } catch (e: Exception) {
            log.info("Build failed for {}", identity())
            val result = TaskCompletedMessage(start = start, end = LocalDateTime.now(), result = BuildExceptionLookup().lookupKeyFromException(e), taskRequest = this)
            globalBus.publish(result)
        }
    }

    override fun toString(): String {
        return identity()
    }


}