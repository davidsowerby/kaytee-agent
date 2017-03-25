package uk.q3c.simplycd.agent.prepare

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.build.BuildPreparationException
import uk.q3c.simplycd.agent.i18n.LabelKey
import uk.q3c.simplycd.agent.i18n.NamedFactory
import uk.q3c.simplycd.agent.queue.GradleTaskExecutor
import uk.q3c.simplycd.agent.queue.GradleTaskRequestFactory
import uk.q3c.simplycd.agent.system.InstallationInfo
import uk.q3c.simplycd.i18n.Named
import uk.q3c.simplycd.i18n.TaskKey
import uk.q3c.simplycd.lifecycle.SimplyCDProjectExtension
import java.io.File

/**
 * Created by David Sowerby on 19 Jan 2017
 */
class DefaultLoadBuildConfiguration @Inject constructor(
        val installationInfo: InstallationInfo,
        val gradleTaskRequestFactory: GradleTaskRequestFactory,
        val gradleTaskExecutor: GradleTaskExecutor,
        namedFactory: NamedFactory
)
    : LoadBuildConfiguration, Named by namedFactory.create(LabelKey.Load_Configuration) {


    /**
     *
     * The second part reads in the configuration
     *
     */
    override fun execute(build: Build) {
        val fileLocation = "build/simplycd.json"
        val inputFile = File(installationInfo.projectInstanceDir(build), fileLocation)
        try {
            // call Gradle task to extract the config
            gradleTaskExecutor.execute(build, TaskKey.Extract_Gradle_Configuration)
            // import from the file generated above
            val configuration = ObjectMapper().readValue(inputFile, SimplyCDProjectExtension::class.java)
            build.configure(configuration)
        } catch (e: Exception) {
            val msg = "Unable to load configuration from $inputFile"
            throw BuildPreparationException(msg, e)
        }
    }
}