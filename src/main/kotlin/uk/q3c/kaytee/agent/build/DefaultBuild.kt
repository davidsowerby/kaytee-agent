package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.assistedinject.Assisted
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Invoke
import net.engio.mbassy.listener.Listener
import org.gradle.tooling.BuildLauncher
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.app.delegatedLifecycle
import uk.q3c.kaytee.agent.app.standardLifecycle
import uk.q3c.kaytee.agent.eventbus.BusMessage
import uk.q3c.kaytee.agent.eventbus.GlobalBus
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.eventbus.SubscribeTo
import uk.q3c.kaytee.agent.prepare.LoadBuildConfiguration
import uk.q3c.kaytee.agent.prepare.PreparationStage
import uk.q3c.kaytee.agent.queue.*
import uk.q3c.kaytee.plugin.GroupConfig
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.kaytee.plugin.TaskKey
import uk.q3c.kaytee.plugin.TaskKey.*
import java.io.File
import java.util.*
import javax.annotation.concurrent.ThreadSafe

/**
 * Uses [BuildFactory] for construction
 *
 * Needs to be thread safe becuase it processes messages from the event bus, which are despatched asynchronously.
 *
 * It is also possible that other methods will be accessed by different threads (from different runners) - although it is unlikely
 * that there will be contention within those calls, it is considered better to be sure that there will not be.
 *
 * Created by David Sowerby on 14 Jan 2017
 */
@ThreadSafe
@Listener @SubscribeTo(GlobalBus::class)
class DefaultBuild @Inject constructor(
        val preparationStage: PreparationStage,
        val buildNumberReader: BuildNumberReader,
        val requestQueue: RequestQueue,
        val globalBusProvider: GlobalBusProvider,
        val gradleTaskRunnerFactory: GradleTaskRunnerFactory,
        val manualTaskRunnerFactory: ManualTaskRunnerFactory,
        val delegatedProjectTaskRunnerFactory: DelegatedProjectTaskRunnerFactory,
        val issueCreatorProvider: Provider<IssueCreator>,
        val buildRecordWriter: BuildRecordWriter,
        @Assisted override val buildRunner: BuildRunner) :

        Build,
        ProjectInstance by buildRunner {


    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override var raiseIssueOnFail: Boolean = false
    lateinit override var stderrOutputFile: File
    lateinit override var stdoutOutputFile: File
    lateinit override var parentBuild: Build
    lateinit override var gradleLauncher: BuildLauncher
    lateinit override var lifecycle: List<TaskKey>
    private var failed = true


    private val completedTasks: MutableList<TaskKey> = mutableListOf()
    private val taskRunners = ArrayDeque<TaskRunner>()
    private val lock = Any()
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
        synchronized(lock) {
            completedTasks.add(taskKey)
            if (passed) {
                if (completedTasks.size < generatedTaskRunners) {
                    pushTaskToRequestQueue()
                } else {
                    passBuild()
                }
            } else {
                failBuild(busMessage as TaskFailedMessage)
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
     *
     * Disabled tasks have results set to NOT_REQUIRED to make it clear that the task was disabled as opposed just not being run
     */
    override fun configure(configuration: KayTeeExtension) {
        synchronized(lock) {
            configuration.validate()
            generateTasks(configuration)
            raiseIssueOnFail = configuration.raiseIssueOnFail
        }
    }


    private fun pushTaskToRequestQueue() {
        if (taskRunners.isNotEmpty()) {
            val taskRunner = taskRunners.poll()
            log.debug("Build {} is adding task '{}' to queue for processing", buildRunner.uid, taskRunner.identity())
            requestQueue.addRequest(taskRunner)
        } else {
            log.error("Attempted to push task from empty taskRunners {}", buildRunner.identity())
        }
    }

    private fun generateTasks(configuration: KayTeeExtension) {
        log.debug("generating task for $buildRunner")
        if (buildRunner.delegated) {
            lifecycle = delegatedLifecycle
            generateCustomTask(buildRunner.delegateTask)
        } else {
            lifecycle = standardLifecycle
            for (task in lifecycle) {
                generateTask(configuration, task)
            }
        }
    }

    private fun generateCustomTask(delegateTask: String) {
        log.debug("Generating TaskRunner for task '$delegateTask' in build ${buildRunner.uid}")
        val taskRunner = gradleTaskRunnerFactory.create(this, Custom, false)
        taskRunners.add(taskRunner)
    }

    private fun generateTask(configuration: KayTeeExtension, taskKey: TaskKey) {
        val uid = buildRunner.uid
        val delegated = buildRunner.delegated
        when (taskKey) {
            Unit_Test, Integration_Test, Functional_Test, Acceptance_Test, Production_Test -> generateTestGroupTask(configuration, taskKey)
            Publish_to_Local -> createLocalGradleTask(taskKey, false)
            Generate_Build_Info -> optionalTask(uid, taskKey, configuration.generateBuildInfo, delegated)

            Extract_Gradle_Configuration -> createLocalGradleTask(taskKey, false) // not normally expected here but does no harm
            Generate_Change_Log -> optionalTask(uid, taskKey, configuration.generateChangeLog, delegated)
            Merge_to_Master -> optionalTask(uid, taskKey, configuration.release.mergeToMaster, delegated)
            Bintray_Upload -> optionalTask(uid, taskKey, configuration.release.toBintray, delegated)
            Custom -> throw InvalidTaskException(taskKey, "Custom task should call generateCustomTask()")
            Version_Check -> throw InvalidTaskException(taskKey, "VersionCheck is called from within LoadBuildConfiguration and should not be executed directly")
            Tag -> optionalTask(uid, taskKey, configuration.versionTag, delegated)
        }
    }


    private fun optionalTask(uid: UUID, taskKey: TaskKey, enabled: Boolean, delegated: Boolean) {
        if (enabled) {
            createLocalGradleTask(taskKey, false)
        } else {
            val msg = TaskNotRequiredMessage(uid, taskKey, delegated)
            globalBusProvider.get().publishAsync(msg)
        }
    }

    private fun generateTestGroupTask(configuration: KayTeeExtension, taskKey: TaskKey) {
        val config: GroupConfig = configuration.testConfig(taskKey)
        // not enabled at all, nothing to do
        if (!config.enabled) {
            log.debug("Test group is disabled: {}", taskKey)
            val msg = TaskNotRequiredMessage(buildRunner.uid, taskKey, false)
            globalBusProvider.get().publishAsync(msg)
            return
        }

        // if quality gate is enabled, we need to invoke the Gradle quality gate task rather than the test itself
        // but that is managed within the GradleTaskRunner
        val actualTaskKey = taskKey

        // if an auto step, we then need to know whether it is local Gradle,or a delegated task
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

    @Handler(delivery = Invoke.Asynchronously)
    fun taskCompleted(message: TaskSuccessfulMessage) {
        synchronized(lock) {
            // filter for messages which apply to this build - probably could make better use of MBassador filtering
            if (message.buildRequestId == this.buildRunner.uid) {
                log.debug("{} received task successful message for task: {}", this, message.taskKey)
                closeTask(message.taskKey, true, message)
            }
        }
    }

    @Handler(delivery = Invoke.Asynchronously)
    fun taskCompleted(message: TaskFailedMessage) {
        synchronized(lock) {
            // filter for messages which apply to this build - probably could make better use of MBassador filtering
            if (message.buildRequestId == this.buildRunner.uid) {
                log.debug("{} received task failed message for task: {}", this, message.taskKey)
                closeTask(message.taskKey, false, message)
            }
        }
    }


    private fun failBuild(busMessage: TaskFailedMessage) {
        val exception = TaskException(busMessage.stdOut)
        failBuild(exception)
    }

    private fun failBuild(exception: Exception) {
        globalBusProvider.get().publishAsync(BuildFailedMessage(buildRunner.uid, buildRunner.delegated, exception))
        closeBuild()
    }

    private fun passBuild() {
        failed = false
        globalBusProvider.get().publishAsync(BuildSuccessfulMessage(buildRunner.uid, buildRunner.delegated))
        closeBuild()
    }

    private fun closeBuild() {
        if (failed && raiseIssueOnFail) {
            issueCreatorProvider.get().raiseIssue(this)
        }
        buildRecordWriter.write(this)
        log.info("Build {} closed, sending BuildProcessCompletedMessage", buildRunner.uid)
        globalBusProvider.get().publishAsync(BuildProcessCompletedMessage(buildRunner.uid, buildRunner.delegated))
    }


    /**
     * Creates a [DelegatedProjectTaskRunner] and adds it to [taskRunners]
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

    override fun tasksWaiting(): List<TaskKey> {
        synchronized(lock) {
            val taskKeys: MutableList<TaskKey> = mutableListOf()
            for (taskRunner in taskRunners) {
                taskKeys.add(taskRunner.taskKey)
            }
            return taskKeys
        }
    }

    override fun toString(): String {
        return "Build ${buildRunner.uid} for project ${project.fullProjectName}"
    }

    override fun execute() {
        log.info("starting build {} ", this)
        try {
            preparationStage.execute(this)
        } catch (e: Exception) {
            synchronized(lock) {
                log.debug("Build {}, preparation failed", this, e)
                val msg = PreparationFailedMessage(buildRunner.uid, buildRunner.delegated, e)
                globalBusProvider.get().publishAsync(msg)
                closeBuild()
                return
            }
        }
        synchronized(lock) {
            generatedTaskRunners = taskRunners.size
            log.debug("Build {} has {} tasks to execute", this, generatedTaskRunners)
            if (taskRunners.isNotEmpty()) {
                // in effect this starts the build proper - the first task is placed into the queue, and as the task requests are
                // completed, another is pushed to the request queue until the build completes or fails
                pushTaskToRequestQueue()
                globalBusProvider.get().publishAsync(BuildStartedMessage(buildRunner.uid, buildRunner.delegated, buildNumber))
            } else {
                // there is nothing to do
                globalBusProvider.get().publishAsync(BuildFailedMessage(buildRunner.uid, buildRunner.delegated, BuildConfigurationException()))
            }
        }
    }


}

class TaskException(msg: String) : RuntimeException(msg)

class BuildConfigurationException : RuntimeException("There were no tasks to carry out, check the kaytee configuration in build.gradle, they may all be disabled")
