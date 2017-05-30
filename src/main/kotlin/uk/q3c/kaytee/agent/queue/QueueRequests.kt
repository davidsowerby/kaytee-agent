package uk.q3c.kaytee.agent.queue

import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.project.Project
import java.util.*

/**
 * Common interface for both [BuildRunner] and [TaskRunner].  Both use the same [RequestQueue]
 * Created by David Sowerby on 25 Jan 2017
 */
interface QueueRequest : Runnable {
    fun identity(): String
}


interface BuildRunner : ProjectInstance, QueueRequest {
    val uid: UUID
}


interface TaskRunner : QueueRequest {
    val build: Build
    val taskKey: TaskKey
}

interface GradleTaskRunner : TaskRunner
interface ManualTaskRunner : TaskRunner


interface ProjectInstance {
    val gitHash: GitSHA
    val project: Project
}

interface GradleTaskRunnerFactory {
    fun create(build: Build, taskKey: TaskKey, includeQualityGate: Boolean): GradleTaskRunner
}

interface ManualTaskRunnerFactory {
    fun create(build: Build, taskKey: TaskKey): ManualTaskRunner
}