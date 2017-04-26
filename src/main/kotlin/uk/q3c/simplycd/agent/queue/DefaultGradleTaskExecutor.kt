package uk.q3c.simplycd.agent.queue

import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.i18n.TaskKey
import uk.q3c.simplycd.agent.i18n.TaskNameMap
import java.io.FileOutputStream

/**
 * Created by David Sowerby on 25 Mar 2017
 */
class DefaultGradleTaskExecutor @Inject constructor(val taskNameMap: TaskNameMap) : GradleTaskExecutor {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun execute(build: Build, taskKey: TaskKey, includeQualityGate: Boolean) {
        val tasks = Splitter.on(CharMatcher.WHITESPACE)
                .omitEmptyStrings()
                .split(taskNameMap.get(taskKey, includeQualityGate))

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
                log.info("Executing Gradle task request for {}, with Gradle command: '{}'", taskKey, tasks)
                build.gradleLauncher.run()
            }
        }
    }
}