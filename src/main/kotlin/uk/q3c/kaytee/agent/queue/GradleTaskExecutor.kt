package uk.q3c.kaytee.agent.queue

import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.i18n.TaskKey

/**
 * Executes a gradle task (or tasks) for the supplied [Build]
 *
 * Created by David Sowerby on 25 Mar 2017
 */
interface GradleTaskExecutor {
    fun execute(build: Build, taskKey: TaskKey, includeQualityGate: Boolean)
}