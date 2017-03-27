package uk.q3c.simplycd.agent.queue

import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.i18n.TaskKey

/**
 * Executes a gradle task (or tasks) for the supplied [Build]
 *
 * Created by David Sowerby on 25 Mar 2017
 */
interface GradleTaskExecutor {
    fun execute(build: Build, taskKey: TaskKey)
}