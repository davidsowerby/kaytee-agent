package uk.q3c.kaytee.agent.build

import org.gradle.tooling.BuildLauncher
import uk.q3c.kaytee.agent.queue.BuildRunner
import uk.q3c.kaytee.agent.queue.ProjectInstance
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.kaytee.plugin.TaskKey
import uk.q3c.util.version.VersionNumber
import java.io.File

/**
 *
 * A [Build] instance acts as the focal point for the asynchronous process of executing build tasks. There are different
 * execution methods, but all are executed asynchronously.
 *
 * When [configure] is called with the configuration taken from build.gradle, individual [TaskRunner]s are generated.
 * The first task is then released to the [RequestQueue] to start the build
 *
 * When a task completes successfully, or fails, a result is returned to the owning Build.  The build will then release
 * the next task(s) to the task queue.  Currently there is only ever a single task released ... future versions may enable
 * parallel execution by releasing multiple tasks - for example to execute two long-running integration tests in parallel.
 *
 * There does not seem to be a valid case for parallel step execution within the lifecycle definition itself
 * (https://www.youtube.com/watch?v=V0FpbDkKYtA) and (https://docs.google.com/drawings/d/1M-O7jO4ks0gc1BlW6MZgAIhhubeHkG_v0_frXbvyggI)
 *
 * Some projects may use a second project (with associated GitHub repository) for example, for functional testing.
 * In this case, a 'child' build is invoked and treated as a task of the 'parent' build
 *
 * A null parent build means that the instance is a 'primary' build
 *
 * Created by David Sowerby on 07 Jan 2017
 */
interface Build : ProjectInstance {
    /**
     * Raise an issue if a build fails.  Generally set by the[KayTeeExtension] passed to [configure]
     */
    var raiseIssueOnFail: Boolean
    var version: VersionNumber
    val buildRunner: BuildRunner
    var parentBuild: Build

    /**
     * The lifecycle used by this build, can only be standard or delegated currently
     */
    var lifecycle: List<TaskKey>

    fun buildNumber(): String

    /**
     * Configures the build as defined by the [KayTeeExtension] provided by the 'kaytee-plugin' Gradle plugin
     * The build.gradle file sets up the configuration, and it is used here to specify how the build should operate -
     * for example, which build steps are enabled.
     *
     * Specific task requests are generated and the first one added to the [RequestQueue] to start the build process
     */
    fun configure(configuration: KayTeeExtension)

    var gradleLauncher: BuildLauncher


    /**
     * Prepares the build environment and sets up [TaskRunner]s for the build
     */
    fun execute()

    var stderrOutputFile: File
    var stdoutOutputFile: File

    /**
     * Returns a list of task keys still to run.  This will exclude any tasks which have been passed to the main queue,
     * even though they may not yet been executed
     */
    fun tasksWaiting(): List<TaskKey>

    /**
     * Returns the version being used - this is drawn from the base version provided by the plugin configuration, plus the
     * short git hash code
     */
    fun version(): String
}