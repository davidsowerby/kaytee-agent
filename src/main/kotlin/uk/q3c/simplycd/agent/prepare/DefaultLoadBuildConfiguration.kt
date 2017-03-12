package uk.q3c.simplycd.agent.prepare

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import uk.q3c.simplycd.agent.build.Build
import uk.q3c.simplycd.agent.build.BuildPreparationException
import uk.q3c.simplycd.agent.i18n.LabelKey
import uk.q3c.simplycd.agent.i18n.NamedFactory
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
        namedFactory: NamedFactory
)
    : LoadBuildConfiguration, Named by namedFactory.create(LabelKey.Load_Configuration) {


    /**
     * We cannot access Gradle extensions via the Tools API, so we extract the SimplyCD configuration by using
     * the *simplycdConfigToJson* task provided by Plugin *simplycd-lifecycle"
     *
     * The second part reads in the configuration
     *
     */
    override fun execute(build: Build) {
        val fileLocation = "build/simplycd.json"
        try {

            // call Gradle task to extract the config
            val extractStep = gradleTaskRequestFactory.create(build, TaskKey.Extract_Gradle_Configuration)
            extractStep.run()

            // import from the file generated above
            val inputFile = File(installationInfo.codeDir(build), fileLocation)
            build.configure(ObjectMapper().readValue(inputFile, SimplyCDProjectExtension::class.java))
        } catch (e: Exception) {
            val msg = "Unable to load configuration from $fileLocation"
            throw BuildPreparationException(msg, e)
        }
    }
}