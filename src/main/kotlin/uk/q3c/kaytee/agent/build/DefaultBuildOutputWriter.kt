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
class DefaultBuildOutputWriter @Inject constructor(val installationInfo: InstallationInfo, val objectMapper: ObjectMapper, val buildRecordCollator: BuildRecordCollator, val fileUtils: FileKUtils) : BuildOutputWriter {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun write(build: Build) {
        val buildOutputDir = installationInfo.buildOutputDir(build)
        val uid = build.buildRunner.uid
        if (!buildOutputDir.exists()) {
            try {
                fileUtils.forceMkdir(buildOutputDir)
            } catch (e: Exception) {
                log.error("Unable create directory $buildOutputDir", e)
                return
            }
        }
        try {
            val buildRecord = buildRecordCollator.getRecord(uid)
            val recordFile = File(buildOutputDir, "buildRecord.json")
            objectMapper.writeValue(recordFile, buildRecord)

            val stacktraceFile = File(buildOutputDir, "stacktrace.txt")
            fileUtils.write(stacktraceFile, buildRecord.stacktrace)

            val buildInfoFile = File(buildOutputDir, "buildInfo.txt")
            fileUtils.write(buildInfoFile, build.version())

        } catch (e: Exception) {
            log.error("Unable to write build record for $uid", e)
        }


    }
}