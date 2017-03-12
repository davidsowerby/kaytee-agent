package uk.q3c.simplycd.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.i18n.TaskKey

/**
 * Created by David Sowerby on 30 Jan 2017
 */
class DefaultManualTaskRequest @Inject constructor(globalBusProvider: GlobalBusProvider, val manualTaskLauncher: ManualTaskLauncher, @Assisted build: Build, @Assisted taskKey: TaskKey) :

        ManualTaskRequest,
        AbstractTaskRequest(build, taskKey, globalBusProvider.get()) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    /**
     * Sends a request to the manual approval handler (expected to be a separate microservice).  The same service would reply with
     * a pass / fail message to the Build responsible for [taskRequest]
     */
    override fun doRun() {
        manualTaskLauncher.run(this)
    }

}