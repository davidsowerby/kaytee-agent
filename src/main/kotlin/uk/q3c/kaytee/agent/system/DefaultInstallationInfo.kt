package uk.q3c.kaytee.agent.system

import com.google.inject.Inject
import com.google.inject.Singleton
import uk.q3c.kaytee.agent.app.baseDirFolderName
import uk.q3c.kaytee.agent.app.baseDir_propertyName
import uk.q3c.kaytee.agent.app.defaultDevelopmentBaseDir
import uk.q3c.kaytee.agent.app.developmentMode_propertyName
import uk.q3c.kaytee.agent.build.Build
import java.io.File

/**
 * Created by David Sowerby on 13 Jan 2017
 */
@Singleton
class DefaultInstallationInfo @Inject constructor() : InstallationInfo {

    override var dataDirRoot = File(System.getProperty("user.home"))
        get() {
            return BaseDirectoryReader.baseDir()
        }
    override var installDirRoot = File(System.getProperty("user.home"))


    override fun gradleStdErrFile(build: Build): File {
        return File(gradleOutputDir(build), "stderr.txt")
    }

    override fun gradleStdOutFile(build: Build): File {
        return File(gradleOutputDir(build), "stdout.txt")
    }

    override fun projectDir(build: Build): File {
        return File(dataDirRoot, build.buildRunner.project.shortProjectName)
    }

    override fun dataDir(): File {
        return dataDirRoot
    }

    override fun installDir(): File {
        return File(installDirRoot, "kaytee")
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
        return File(buildNumberDir(build), build.buildRunner.project.shortProjectName)
    }
}

/**
 * Identifies the root directory being used by Ratpack and KayTee for data
 */
object BaseDirectoryReader {

    fun baseDir(): File {
        val developmentMode = System.getProperty(developmentMode_propertyName, "true").toBoolean()
        val baseDirName = System.getProperty(baseDir_propertyName)
        if (baseDirName == null) {
            val defaultBaseDir = if (developmentMode) {
                File(defaultDevelopmentBaseDir)
            } else {
                val userHome = File(System.getProperty("user.home"))
                File(userHome, baseDirFolderName)
            }
            return defaultBaseDir
        } else {
            return File(baseDirName)
        }
    }
}