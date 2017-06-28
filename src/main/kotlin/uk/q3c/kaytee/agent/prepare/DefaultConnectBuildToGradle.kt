package uk.q3c.kaytee.agent.prepare

import com.google.inject.Inject
import org.apache.commons.io.FileUtils
import org.gradle.tooling.GradleConnector
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.i18n.LabelKey
import uk.q3c.kaytee.agent.i18n.Named
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.queue.RequestQueue
import uk.q3c.kaytee.agent.system.InstallationInfo
import java.io.IOException

/**
 * Created by David Sowerby on 26 Jan 2017
 */
class DefaultConnectBuildToGradle @Inject constructor(val requestQueue: RequestQueue, val installationInfo: InstallationInfo, namedFactory: NamedFactory) :
        ConnectBuildToGradle,
        Named by namedFactory.create(LabelKey.Connect_Build_to_Gradle) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun execute(build: Build) {
        log.debug("Connecting to Gradle project")
        val projectDir = installationInfo.projectInstanceDir(build)
        val connector = GradleConnector.newConnector()
        connector.forProjectDirectory(projectDir)
        // we need a way of stopping the Gradle build externally
        requestQueue.stoppers.put(build.buildRunner, GradleConnector.newCancellationTokenSource())
        val connection = connector.connect()
        log.info("Gradle connected to build at {}", projectDir)


        val gradleOutputDir = installationInfo.buildOutputDir(build)
        if (!gradleOutputDir.exists()) {
            FileUtils.forceMkdir(gradleOutputDir)
        }

        val stderrOutputFile = installationInfo.gradleStdErrFile(build)
        val stdoutOutputFile = installationInfo.gradleStdOutFile(build)


        stderrOutputFile.createNewFile()
        if (!stderrOutputFile.exists()) {
            throw IOException("Unable to create file for stderr")
        }

        stdoutOutputFile.createNewFile()
        if (!stdoutOutputFile.exists()) {
            throw IOException("Unable to create files for stdout")
        }

        build.stderrOutputFile = stderrOutputFile
        build.stdoutOutputFile = stdoutOutputFile
        build.gradleLauncher = connection.newBuild()
    }
}