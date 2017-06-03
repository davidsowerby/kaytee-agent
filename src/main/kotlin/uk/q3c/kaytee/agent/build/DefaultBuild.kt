package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import org.gradle.tooling.BuildLauncher
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.eventbus.GlobalBus
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.eventbus.SubscribeTo
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.i18n.TaskKey.*
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey
import uk.q3c.kaytee.agent.prepare.PreparationStage
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.kaytee.plugin.GroupConfig
import uk.q3c.kaytee.plugin.KayTeeExtension
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
        val gradleTaskRunnerFactory: GradleTaskRunnerFactory,
        val manualTaskRunnerFactory: ManualTaskRunnerFactory,
        @Assisted override val buildRunner: BuildRunner) :

        Build,
        ProjectInstance by buildRunner {


    private val log = LoggerFactory.getLogger(this.javaClass.name)

    lateinit override var stderrOutputFile: File
    lateinit override var stdoutOutputFile: File
    lateinit override var parentBuild: Build
    lateinit override var gradleLauncher: BuildLauncher

    private val results: MutableList<TaskKey> = mutableListOf()
    private val taskRunners = ArrayDeque<TaskRunner>()
    private val completedTasksLock = Any()
    private var generatedTaskRunners: Int = 0

    private var buildNumber: String = ""
        get() {
            if (field.isEmpty()) {
                buildNumber = buildNumberReader.nextBuildNumber(this)
            }
            return field
        }


    private fun closeTask(taskKey: TaskKey, passed: Boolean, busMessage: BusMessage) {
        log.debug("Build {} is closing task {}", buildRunner.uid, taskKey)
        synchronized(completedTasksLock) {
            results.add(taskKey)
            if (passed) {
                if (results.size < generatedTaskRunners) {
                    pushTaskToRequestQueue()
                } else {
                    closeBuild(true, busMessage)
                }
            } else {
                closeBuild(false, busMessage)
            }
        }

    }


    override fun buildNumber(): String {
        return buildNumber
    }


    /**
     * Sets the configuration for this Build.  Configuration is extracted from the build.gradle file in the
     * preparation stage by the [LoadBuildConfiguration] step.
     *
     * Each enabled step produces one or more tasks (usually there is only one task per step, but if
     * both *auto* and *manual* properties are set, two tasks are created, with the auto task to run first
     */
    override fun configure(configuration: KayTeeExtension) {
        synchronized(taskRunners) {
            setupTasks(configuration.unitTest, Unit_Test)
            setupTasks(configuration.integrationTest, Integration_Test)
            setupTasks(configuration, Generate_Build_Info, Generate_Change_Log, Local_Publish)
            setupTasks(configuration.functionalTest, Functional_Test)
            setupTasks(configuration.acceptanceTest, Acceptance_Test)
            setupTasks(configuration, Merge_to_Master, Bintray_Upload)
            setupTasks(configuration.productionTest, Production_Test)
        }

    }


    private fun pushTaskToRequestQueue() {
        synchronized(completedTasksLock) {
            if (taskRunners.isNotEmpty()) {
                val taskRunner = taskRunners.poll()
                log.debug("Build {} is adding task '{}' to queue for processing", buildRunner.uid, taskRunner.identity())
                requestQueue.addRequest(taskRunner)
            } else {
                log.error("Attempted to push task from empty taskRunners {}", buildRunner.identity())
            }
        }
    }

    private fun setupTasks(configuration: KayTeeExtension, vararg taskKeys: TaskKey) {
        for (taskKey in taskKeys) {
            when (taskKey) {
                Unit_Test, Integration_Test, Functional_Test, Acceptance_Test, Production_Test -> throw IllegalArgumentException("Test tasks should be set up using the call with GroupConfig")
                Local_Publish -> createLocalGradleTask(taskKey, false)
                Generate_Build_Info -> optionalTask(taskKey, configuration.generateBuildInfo)

                TaskKey.Extract_Gradle_Configuration -> createLocalGradleTask(taskKey, false) // not normally expected here but does no harm
                TaskKey.Generate_Change_Log -> optionalTask(taskKey, configuration.generateChangeLog)
                TaskKey.Merge_to_Master -> optionalTask(taskKey, configuration.release.mergeToMaster)
                TaskKey.Bintray_Upload -> optionalTask(taskKey, configuration.release.toBintray)
            }
        }
    }

    private fun optionalTask(taskKey: TaskKey, optionValue: Boolean) {
        if (optionValue) {
            createLocalGradleTask(taskKey, false)
        }
    }

    private fun setupTasks(config: GroupConfig, taskKey: TaskKey) {
        // not enabled at all, nothing to do
        if (!config.enabled) {
            log.debug("Config is set 'disabled'")
            return
        }

        // if quality gate is enabled, we need to invoke the Gradle quality gate task rather than the test itself
        // but that is managed within the GradleTaskRunner
        val actualTaskKey = taskKey

        // if an auto step, we then need to know whether it is local Gradle,or a sub build
        if (config.auto) {
            if (config.external) {
                createSubBuildTask(actualTaskKey, config)
            } else {
                createLocalGradleTask(actualTaskKey, config.qualityGate)
            }
        }

        // it is possible that a step is both auto and manual
        if (config.manual) {
            createManualTask(actualTaskKey, config)
        }

    }

    @Handler
    fun taskCompleted(message: TaskSuccessfulMessage) {
        // filter for messages which apply to this build - probably could make better use of MBassador filtering
        if (message.buildRequestId == this.buildRunner.uid) {
            log.debug("Build {} received task successful message for task: {}", this.buildRunner.uid, message.taskKey)
            closeTask(message.taskKey, true, message)
        }
    }

    @Handler
    fun taskCompleted(message: TaskFailedMessage) {
        // filter for messages which apply to this build - probably could make better use of MBassador filtering
        if (message.buildRequestId == this.buildRunner.uid) {
            log.debug("Build {} received task failed message for task: {}", this.buildRunner.uid, message.taskKey)
            closeTask(message.taskKey, false, message)
        }
    }


    private fun closeBuild(passed: Boolean, busMessage: BusMessage) {
        if (passed) {
            globalBusProvider.get().publish(BuildSuccessfulMessage(buildRunner.uid))
        } else {
            if (busMessage is TaskFailedMessage) {
                val exception = TaskException(busMessage.result)
                globalBusProvider.get().publish(BuildFailedMessage(buildRunner.uid, exception))
            } else {
                throw QueueException("Build ${buildRunner.uid}, only a TaskFailedMessage should get this far, but received a ${busMessage.javaClass.simpleName}")
            }
        }
        log.info("Closing build for {}, build {}", project.shortProjectName, buildRunner.uid)

    }


    /**
     * Creates a [SubBuildTask] and adds it to [taskRunners]
     */
    private fun createSubBuildTask(taskNameKey: TaskKey, config: GroupConfig) {
        TODO()
    }

    /**
     * Creates a [GradleTask] and adds it to [taskRunners]
     */
    private fun createLocalGradleTask(taskKey: TaskKey, includeQualityGate: Boolean) {
        val taskRunner = gradleTaskRunnerFactory.create(build = this, taskKey = taskKey, includeQualityGate = includeQualityGate)
        taskRunners.add(taskRunner)
    }

    /**
     * Creates a [ManualTask] and adds it to [taskRunners]
     */
    private fun createManualTask(taskKey: TaskKey, config: GroupConfig) {
        val taskRunner = manualTaskRunnerFactory.create(build = this, taskKey = taskKey)
        taskRunners.add(taskRunner)
    }


    override fun execute() {
//        log.info("starting build")
        log.info("starting build {} for project: {}", buildNumber, project.shortProjectName)
        try {
            preparationStage.execute(this)
        } catch (e: Exception) {
            log.debug("preparation failed", e)
            globalBusProvider.get().publish(PreparationFailedMessage(buildRunner.uid, e))
            return
        }
        generatedTaskRunners = taskRunners.size
        log.debug("Build {} has {} tasks to execute", buildRunner.identity(), generatedTaskRunners)
        if (taskRunners.isNotEmpty()) {
            // in effect this starts the build proper - the first task is placed into the queue, and as the task requests are
            // completed, another is pushed to the request queue until the build completes or fails
            pushTaskToRequestQueue()
            globalBusProvider.get().publish(BuildStartedMessage(buildRunner.uid, buildNumber))
        } else {
            // there is nothing to do
            val msg = "There were no tasks to carry out, check the kaytee configuration in build.gradle, they may all be disabled"
            globalBusProvider.get().publish(BuildFailedMessage(buildRunner.uid, BuildConfigurationException()))
        }
    }


}

class TaskException(result: TaskResultStateKey) : RuntimeException(result.name)

class BuildConfigurationException : RuntimeException("There were no tasks to carry out, check the kaytee configuration in build.gradle, they may all be disabled")
