package uk.q3c.simplycd.agent.system

import com.google.inject.Inject
import com.google.inject.Singleton
import uk.q3c.simplycd.agent.build.Build
import java.io.File

/**
 * Created by David Sowerby on 13 Jan 2017
 */
@Singleton
class DefaultInstallationInfo @Inject constructor() : InstallationInfo {

    override var dataDirRoot = File(System.getProperty("user.home"))
    override var installDirRoot = File(System.getProperty("user.home"))


    override fun gradleStdErrFile(build: Build): File {
        return File(gradleOutputDir(build), "stderr.txt")
    }

    override fun gradleStdOutFile(build: Build): File {
        return File(gradleOutputDir(build), "stdout.txt")
    }

    override fun projectDir(build: Build): File {
        return File(dataDir(), build.buildRequest.project.shortProjectName)
    }

    override fun dataDir(): File {
        return File(dataDirRoot, "simplycd-data")
    }

    override fun installDir(): File {
        return File(installDirRoot, "simplycd")
    }

    private fun projectBuildNumberDir(build: Build): File {
        return File(projectDir(build), build.buildNumber().toString())
    }

    override fun gradleOutputDir(build: Build): File {
        return File(projectBuildNumberDir(build), "build-output")
    }

    override fun buildNumberDir(build: Build): File {
        return projectBuildNumberDir(build)
    }

    override fun projectInstanceDir(build: Build): File {
        return File(buildNumberDir(build), build.buildRequest.project.shortProjectName)
    }
}