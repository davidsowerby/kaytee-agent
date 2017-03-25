package uk.q3c.simplycd.agent.build

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.gradle.tooling.BuildLauncher
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.eventbus.GlobalBus
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.agent.eventbus.SubscribeTo
import uk.q3c.simplycd.agent.prepare.PreparationStage
import uk.q3c.simplycd.agent.queue.*
import uk.q3c.simplycd.i18n.TaskKey
import uk.q3c.simplycd.lifecycle.SimplyCDProjectExtension
import java.io.File
import java.util.*

/**
 * Uses [BuildFactory] for construction
 *
 * Created by David Sowerby on 14 Jan 2017
 */
@Listener @SubscribeTo(GlobalBus::class)
class DefaultBuild @Inject constructor(
        val preparationStage: PreparationStage,
        val buildNumberReader: BuildNumberReader,
        val requestQueue: RequestQueue,
        val globalBusProvider: GlobalBusProvider,
        val gradleTaskRequestFactory: GradleTaskRequestFactory,
        val manualTaskRequestFactory: ManualTaskRequestFactory,
        @Assisted override val buildRequest: BuildRequest) :

        Build,
        ProjectInstance by buildRequest {


    private val log = LoggerFactory.getLogger(this.javaClass.name)

    lateinit override var stderrOutputFile: File
    lateinit override var stdoutOutputFile: File
    lateinit override var parentBuild: Build
    lateinit override var gradleLauncher: BuildLauncher


    val results: MutableList<BuildResult> = mutableListOf()
    private val taskRequests = ArrayDeque<TaskRequest>()
    private val resultLock = Any()
    private var generatedTaskRequests: Int = 0

    private var buildNumber: Int = -1
        get() {
            if (field < 0) {
                buildNumber = buildNumberReader.nextBuildNumber(buildRequest.project.shortProjectName)
            }
            return field
        }


    private fun addResult(buildResult: BuildResult) {
        log.debug("result received for {}", buildResult.queueRequest.identity())
        synchronized(resultLock) {
            results.add(buildResult)
            if (results.size >= generatedTaskRequests) {
                closeBuild()
            } else {
                pushTaskToRequestQueue()
            }
        }

    }


    override fun buildNumber(): Int {
        return buildNumber
    }


    /**
     * Sets the configuration for this Build.  Configuration is extracted from the build.gradle file in the
     * preparation stage by the [LoadBuildConfiguration] step.
     *
     * Each enabled step produces one or more tasks (usually there is only one task per step, but if
     * both *auto* and *manual* properties are set, two tasks are created, with the auto task to run first
     */
    override fun configure(configuration: SimplyCDProjectExtension) {
        synchronized(taskRequests) {
            setupTasks(configuration.unitTest, TaskKey.Unit_Test, TaskKey.Unit_Test_Quality_Gate)
            setupTasks(configuration.integrationTest, TaskKey.Integration_Test, TaskKey.Integration_Test_Quality_Gate)
            setupTasks(configuration.functionalTest, TaskKey.Functional_Test, TaskKey.Functional_Test_Quality_Gate)
            setupTasks(configuration.acceptanceTest, TaskKey.Acceptance_Test, TaskKey.Acceptance_Test_Quality_Gate)
            setupTasks(configuration.productionTest, TaskKey.Production_Test, TaskKey.Production_Test_Quality_Gate)
        }

    }

    private fun pushTaskToRequestQueue() {
        synchronized(resultLock) {
            if (taskRequests.isNotEmpty()) {
                requestQueue.addRequest(taskRequests.poll())
            } else {
                log.error("Attempted to push task from empty taskRequests {}", buildRequest.identity())
            }
        }
    }

    private fun setupTasks(config: SimplyCDProjectExtension.GroupConfig, taskKey: TaskKey, qualityGateKey: TaskKey) {
        // not enabled at all, nothing to do
        if (!config.enabled) {
            return
        }

        // if quality gate is enabled, we only need to call that - it will invoke the associated test task
        val actualTaskKey = if (config.qualityGate) {
            qualityGateKey
        } else {
            taskKey
        }

        // if an auto step, we then need to know whether it is local Gradle,or a sub build
        if (config.auto) {
            if (config.external) {
                createSubBuildTask(actualTaskKey, config)
            } else {
                createLocalGradleTask(actualTaskKey, config)
            }
        }

        // it is possible that a step is both auto and manual
        if (config.manual) {
            createManualTask(actualTaskKey, config)
        }

    }

    @Handler()
    fun taskCompleted(message: TaskCompletedMessage) {
        // filter for messages which apply to this build - probably could make better use of MBassador filtering
        if (message.taskRequest.build == this) {
            log.debug("Received completion message for task: {}", message.taskRequest.identity())
            val result = BuildResult(queueRequest = message.taskRequest, start = message.start, end = message.end, state = message.result)
            addResult(result)
        }
    }


    private fun closeBuild() {
        globalBusProvider.get().publish(BuildCompletedMessage(project, buildNumber, buildRequest))
        log.info("Closing build for {}, build {}", project.shortProjectName, buildNumber)
//        val result =  BuildResult(start = startTime, end = endTime, state = BuildExceptionLookup().lookupKeyFromException(e))

    }


    /**
     * Creates a [SubBuildTask] and adds it to [taskRequests]
     */
    private fun createSubBuildTask(taskNameKey: TaskKey, config: SimplyCDProjectExtension.GroupConfig) {
        TODO()
    }

    /**
     * Creates a [GradleTask] and adds it to [taskRequests]
     */
    private fun createLocalGradleTask(taskKey: TaskKey, config: SimplyCDProjectExtension.GroupConfig) {
        val taskRequest = gradleTaskRequestFactory.create(build = this, taskKey = taskKey)
        taskRequests.add(taskRequest)
    }

    /**
     * Creates a [ManualTask] and adds it to [taskRequests]
     */
    private fun createManualTask(taskKey: TaskKey, config: SimplyCDProjectExtension.GroupConfig) {
        val taskRequest = manualTaskRequestFactory.create(build = this, taskKey = taskKey)
        taskRequests.add(taskRequest)
    }


    override fun execute() {
//        log.info("starting build")
        log.info("starting build {} for project: {}", buildNumber, project.shortProjectName)
        globalBusProvider.get().publish(BuildStartedMessage(buildRequest))
        preparationStage.execute(this)
        generatedTaskRequests = taskRequests.size
        if (taskRequests.isNotEmpty()) {
            // in effect this starts the build proper - the first task is placed into the queue, and as the task requests are
            // completed, another is added until the build completes or fails
            pushTaskToRequestQueue()
        } else {
            // there is nothing to do
            log.warn("There were no tasks to carry out, check the simplycd configuration in build.gradle, they may all be disabled")
            closeBuild()
        }
    }


}