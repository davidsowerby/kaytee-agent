package uk.q3c.kaytee.agent.build

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.util.file.FileKUtils
import java.io.File

/**
 * Created by David Sowerby on 19 Jul 2017
 */
class DefaultBuildRecordWriter @Inject constructor(val installationInfo: InstallationInfo, val objectMapper: ObjectMapper, val buildRecordCollator: BuildRecordCollator, val fileUtils: FileKUtils) : BuildRecordWriter {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun write(build: Build) {
        val buildDir = installationInfo.buildOutputDir(build)
        val uid = build.buildRunner.uid
        if (!buildDir.exists()) {
            try {
                fileUtils.forceMkdir(buildDir)
                val buildRecord = buildRecordCollator.getRecord(uid)
                val file = File(buildDir, "buildRecord.json")
                objectMapper.writeValue(file, buildRecord)
            } catch (e: Exception) {
                log.error("Unable to write build record for $uid", e)

            }
        }

    }
}