package uk.q3c.simplycd.queue

import uk.q3c.build.gitplus.GitSHA
import uk.q3c.simplycd.build.Build
import uk.q3c.simplycd.i18n.TaskKey
import uk.q3c.simplycd.project.Project

/**
 * Common interface for both [BuildRequest] and [TaskRequest].  Both use the same [RequestQueue]
 * Created by David Sowerby on 25 Jan 2017
 */
interface QueueRequest : Runnable {
    fun identity(): String
}


interface BuildRequest : ProjectInstance, QueueRequest


interface TaskRequest : QueueRequest {
    val build: Build
    val taskKey: TaskKey
}

interface GradleTaskRequest : TaskRequest
interface ManualTaskRequest : TaskRequest


interface ProjectInstance {
    val gitHash: GitSHA
    val project: Project
}

interface GradleTaskRequestFactory {
    fun create(build: Build, taskKey: TaskKey): GradleTaskRequest
}

interface ManualTaskRequestFactory {
    fun create(build: Build, taskKey: TaskKey): ManualTaskRequest
}