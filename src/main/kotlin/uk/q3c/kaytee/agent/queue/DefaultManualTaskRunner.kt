package uk.q3c.kaytee.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.TaskKey
import uk.q3c.kaytee.agent.system.InstallationInfo

/**
 * Created by David Sowerby on 30 Jan 2017
 */
class DefaultManualTaskRunner @Inject constructor(
        val globalBusProvider: GlobalBusProvider,
        val manualTaskLauncher: ManualTaskLauncher,
        val installationInfo: InstallationInfo,
        @Assisted override val build: Build,
        @Assisted override val taskKey: TaskKey) :

        ManualTaskRunner {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    /**
     * Sends a request to the manual approval handler (expected to be a separate microservice).  The same service would reply with
     * a pass / fail message to the Build responsible for [TaskRunner]
     */
    override fun run() {
        manualTaskLauncher.run(this)
    }

    override fun identity(): String {
        return "${build.buildRunner.project.shortProjectName}:${build.buildRunner.uid}:$taskKey}"
    }

}