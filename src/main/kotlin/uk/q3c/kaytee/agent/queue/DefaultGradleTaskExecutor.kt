package uk.q3c.kaytee.agent.queue

import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskNameMap
import java.io.FileOutputStream

/**
 * Created by David Sowerby on 25 Mar 2017
 */
class DefaultGradleTaskExecutor @Inject constructor(val taskNameMap: TaskNameMap) : GradleTaskExecutor {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun execute(build: Build, customTaskName: String) {
        log.debug("executing custom task $customTaskName for build ${build.buildRunner.uid}")
        doExecute(build, customTaskName)
    }



    override fun execute(build: Build, taskKey: TaskKey, includeQualityGate: Boolean) {
        val taskName = taskNameMap.get(taskKey, includeQualityGate)
        log.debug("executing standard task $taskName, for task key $taskKey, for build ${build.buildRunner.uid}")
        doExecute(build, taskName)
    }

    private fun doExecute(build: Build, task: String) {
        val tasks = Splitter.on(CharMatcher.WHITESPACE)
                .omitEmptyStrings()
                .split(task)

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
                log.info("Build {} executing Gradle task {}", build.buildRunner.uid, task)
                build.gradleLauncher.run()
            }
        }
    }
}