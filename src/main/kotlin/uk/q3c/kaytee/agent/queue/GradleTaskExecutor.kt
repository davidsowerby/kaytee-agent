package uk.q3c.kaytee.agent.queue

import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Executes a gradle task (or tasks) for the supplied [Build]
 *
 * Created by David Sowerby on 25 Mar 2017
 */
interface GradleTaskExecutor {
    /**
     * Executes a Gradle task
     *
     * @param build the build the task relates to
     * @param the [TaskKey] identifying the task.  If the value of this is [TaskKey.Custom], the Gradle is invoked using [customTaskName]

     */
    fun execute(build: Build, taskKey: TaskKey, includeQualityGate: Boolean)

    /**
     *
     * @param customTaskName used only if [TaskKey] has a value of [TaskKey.Custom], defines the task name to invoke Gradle with.  It can define multiple, space separated Gradle tasks, like the Gradle command line, for example 'clean test'
     */
    fun execute(build: Build, customTaskName: String)
}