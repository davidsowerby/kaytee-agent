package uk.q3c.kaytee.agent.build

import com.google.common.collect.ImmutableList.of
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
import uk.q3c.kaytee.agent.prepare.PreparationStage
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.kaytee.plugin.GroupConfig
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.kaytee.plugin.TaskKey
import uk.q3c.kaytee.plugin.TaskKey.*
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
        val delegatedProjectTaskRunnerFactory: DelegatedProjectTaskRunnerFactory,
        @Assisted override val buildRunner: BuildRunner) :

        Build,
        ProjectInstance by buildRunner {


    private val log = LoggerFactory.getLogger(this.javaClass.name)

    lateinit override var stderrOutputFile: File
    lateinit override var stdoutOutputFile: File
    lateinit override var parentBuild: Build
    lateinit override var gradleLauncher: BuildLauncher

    private val standardLifecycle: List<TaskKey> = of(Unit_Test, Integration_Test, Generate_Build_Info, Generate_Change_Log, Local_Publish, Functional_Test, Acceptance_Test, Merge_to_Master, Bintray_Upload, Production_Test)
    private val delegatedLifecycle: List<TaskKey> = of()

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
            generateTasks(configuration)
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

    private fun generateTasks(configuration: KayTeeExtension) {
        log.debug("generating task for ${buildRunner}")
        if (buildRunner.delegated) {
            generateCustomTask(buildRunner.delegateTask)
        } else {
            for (task in standardLifecycle) {
                generateTask(configuration, task)
            }
        }
    }

    private fun generateCustomTask(delegateTask: String) {
        log.debug("Generating TaskRunner for task '$delegateTask' in build ${buildRunner.uid}")
        val taskRunner = gradleTaskRunnerFactory.create(this, TaskKey.Custom, false)
        taskRunners.add(taskRunner)
    }

    private fun generateTask(configuration: KayTeeExtension, taskKey: TaskKey) {
        when (taskKey) {
            Unit_Test, Integration_Test, Functional_Test, Acceptance_Test, Production_Test -> generateTestGroupTask(configuration, taskKey)
            Local_Publish -> createLocalGradleTask(taskKey, false)
            Generate_Build_Info -> optionalTask(taskKey, configuration.generateBuildInfo)

            Extract_Gradle_Configuration -> createLocalGradleTask(taskKey, false) // not normally expected here but does no harm
            Generate_Change_Log -> optionalTask(taskKey, configuration.generateChangeLog)
            Merge_to_Master -> optionalTask(taskKey, configuration.release.mergeToMaster)
            Bintray_Upload -> optionalTask(taskKey, configuration.release.toBintray)
        }
    }


    private fun optionalTask(taskKey: TaskKey, optionValue: Boolean) {
        if (optionValue) {
            createLocalGradleTask(taskKey, false)
        }
    }

    private fun generateTestGroupTask(configuration: KayTeeExtension, taskKey: TaskKey) {
        val config: GroupConfig = configuration.testConfig(taskKey)
        // not enabled at all, nothing to do
        if (!config.enabled) {
            log.debug("Test group is disabled: ", taskKey)
            return
        }

        // if quality gate is enabled, we need to invoke the Gradle quality gate task rather than the test itself
        // but that is managed within the GradleTaskRunner
        val actualTaskKey = taskKey

        // if an auto step, we then need to know whether it is local Gradle,or a sub build
        if (config.auto) {
            if (config.delegated) {
                createDelegatedProjectTask(actualTaskKey, config)
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
            log.debug("{} received task successful message for task: {}", this, message.taskKey)
            closeTask(message.taskKey, true, message)
        }
    }

    @Handler
    fun taskCompleted(message: TaskFailedMessage) {
        // filter for messages which apply to this build - probably could make better use of MBassador filtering
        if (message.buildRequestId == this.buildRunner.uid) {
            log.debug("{} received task failed message for task: {}", this, message.taskKey)
            closeTask(message.taskKey, false, message)
        }
    }


    private fun closeBuild(passed: Boolean, busMessage: BusMessage) {
        if (passed) {
            globalBusProvider.get().publish(BuildSuccessfulMessage(buildRunner.uid, buildRunner.delegated))
        } else {
            if (busMessage is TaskFailedMessage) {
                val exception = TaskException(busMessage.stdOut)
                globalBusProvider.get().publish(BuildFailedMessage(buildRunner.uid, buildRunner.delegated, exception))
            } else {
                throw QueueException("Build ${buildRunner.uid}, only a TaskFailedMessage should get this far, but received a ${busMessage.javaClass.simpleName}")
            }
        }
        log.info("Closing build for {}, build {}", project.shortProjectName, buildRunner.uid)

    }


    /**
     * Creates a [SubBuildTask] and adds it to [taskRunners]
     */
    private fun createDelegatedProjectTask(taskKey: TaskKey, config: GroupConfig) {
        log.debug("Creating a task runner for {}, type 'DelegatedProject'", taskKey)
        val taskRunner = delegatedProjectTaskRunnerFactory.create(build = this, taskKey = taskKey, groupConfig = config)
        taskRunners.add(taskRunner)
    }

    /**
     * Creates a [GradleTask] and adds it to [taskRunners]
     */
    private fun createLocalGradleTask(taskKey: TaskKey, includeQualityGate: Boolean) {
        log.debug("Creating a task runner for {}, type 'LocalGradle'", taskKey)
        val taskRunner = gradleTaskRunnerFactory.create(build = this, taskKey = taskKey, includeQualityGate = includeQualityGate)
        taskRunners.add(taskRunner)
    }

    /**
     * Creates a [ManualTask] and adds it to [taskRunners]
     */
    private fun createManualTask(taskKey: TaskKey, config: GroupConfig) {
        log.debug("Creating a task runner for {}, type 'Manual'", taskKey)
        val taskRunner = manualTaskRunnerFactory.create(build = this, taskKey = taskKey)
        taskRunners.add(taskRunner)
    }

    override fun toString(): String {
        return "Build ${buildRunner.uid} for project ${project.fullProjectName}"
    }

    override fun execute() {
        log.info("starting build {} ", this)
        try {
            preparationStage.execute(this)
        } catch (e: Exception) {
            log.debug("Build {}, preparation failed", this, e)
            globalBusProvider.get().publish(PreparationFailedMessage(buildRunner.uid, buildRunner.delegated, e))
            return
        }
        generatedTaskRunners = taskRunners.size
        log.debug("Build {} has {} tasks to execute", this, generatedTaskRunners)
        if (taskRunners.isNotEmpty()) {
            // in effect this starts the build proper - the first task is placed into the queue, and as the task requests are
            // completed, another is pushed to the request queue until the build completes or fails
            pushTaskToRequestQueue()
            globalBusProvider.get().publish(BuildStartedMessage(buildRunner.uid, buildRunner.delegated, buildNumber))
        } else {
            // there is nothing to do
            globalBusProvider.get().publish(BuildFailedMessage(buildRunner.uid, buildRunner.delegated, BuildConfigurationException()))
        }
    }


}

class TaskException(msg: String) : RuntimeException(msg)

class BuildConfigurationException : RuntimeException("There were no tasks to carry out, check the kaytee configuration in build.gradle, they may all be disabled")
