package uk.q3c.simplycd.agent.queue

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.agent.i18n.TaskKey
import uk.q3c.simplycd.agent.system.InstallationInfo


/**
 *
 * Represents a single 'task' call to Gradle (though that may contain multiple Gradle tasks, for example 'clean test')
 *
 * Created by David Sowerby on 26 Jan 2017
 */
class DefaultGradleTaskRunner @Inject constructor(
        globalBusProvider: GlobalBusProvider,
        val gradleTaskExecutor: GradleTaskExecutor,
        installationInfo: InstallationInfo,
        @Assisted build: Build,
        @Assisted taskKey: TaskKey,
        @Assisted val includeQualityGate: Boolean) :

        GradleTaskRunner,
        AbstractTaskRunner(build, taskKey, installationInfo, globalBusProvider.get()) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun doRun() {
        gradleTaskExecutor.execute(build, taskKey, includeQualityGate)
    }

    override fun toString(): String {
        return identity()
    }


}