package uk.q3c.kaytee.agent.build

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.BehaviorSpec
import org.apache.commons.io.FileUtils
import uk.q3c.kaytee.agent.queue.BuildRunner
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.util.file.DefaultFileKUtils
import uk.q3c.util.file.FileKUtils
import uk.q3c.util.testutil.LogMonitor
import java.io.File
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by David Sowerby on 20 Jul 2017
 */
class DefaultBuildRecordWriterTest : BehaviorSpec() {


    init {
        Given("proper access to file system") {
            val buildDir: File = createTempDir(prefix = "ktest", suffix = "")
            val uid = UUID.randomUUID()

            val installationInfo: InstallationInfo = mock()
            val build: Build = mock()
            val buildRunner: BuildRunner = mock()
            val fileUtils: FileKUtils = DefaultFileKUtils()

            val buildRecordCollator: BuildRecordCollator = mock()
            val writer = DefaultBuildRecordWriter(installationInfo, ObjectMapper(), buildRecordCollator, fileUtils)
            val record = BuildRecord(uid, OffsetDateTime.now(), false)

            whenever(installationInfo.buildOutputDir(build)).thenReturn(buildDir)
            whenever(build.buildRunner).thenReturn(buildRunner)
            whenever(buildRunner.uid).thenReturn(uid)
            whenever(buildRecordCollator.getRecord(uid)).thenReturn(record)

            When("write is called for existing directory") {
                writer.write(build)

                Then("file is created") {
                    File(buildDir, "buildRecord.json").exists() shouldEqual true
                }
            }

            When("the build output directory does not exist") {
                FileUtils.forceDelete(buildDir)
                writer.write(build)

                Then("directory and file is created") {
                    File(buildDir, "buildRecord.json").exists() shouldEqual true
                }
            }

        }
    }

    init {
        Given("accessing file system fails") {
            val buildDir: File = createTempDir(prefix = "ktest", suffix = "")
            val uid = UUID.randomUUID()

            val installationInfo: InstallationInfo = mock()
            val build: Build = mock()
            val buildRunner: BuildRunner = mock()
            val fileUtils: FileKUtils = mock()

            val buildRecordCollator: BuildRecordCollator = mock()
            val writer = DefaultBuildRecordWriter(installationInfo, ObjectMapper(), buildRecordCollator, fileUtils)
            val record = BuildRecord(uid, OffsetDateTime.now(), false)

            val logMonitor = LogMonitor()
            logMonitor.addClassFilter(DefaultBuildRecordWriter::class.java)

            whenever(installationInfo.buildOutputDir(build)).thenReturn(buildDir)
            whenever(build.buildRunner).thenReturn(buildRunner)
            whenever(buildRunner.uid).thenReturn(uid)
            whenever(buildRecordCollator.getRecord(uid)).thenReturn(record)
            whenever(fileUtils.forceMkdir(buildDir)).thenThrow(RuntimeException("fake"))



            When("the output directory is missing and cannot be created") {
                if (buildDir.exists()) {
                    FileUtils.forceDelete(buildDir)
                }
                writer.write(build)
                Then("logged as error") {
                    File(buildDir, "buildRecord.json").exists() shouldEqual false
                    logMonitor.errorLogs().isEmpty() shouldEqual false
                }
            }
        }
    }

}

