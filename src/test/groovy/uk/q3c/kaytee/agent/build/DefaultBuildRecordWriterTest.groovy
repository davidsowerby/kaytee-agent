package uk.q3c.kaytee.agent.build

import com.fasterxml.jackson.databind.ObjectMapper
import org.codehaus.plexus.util.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.kaytee.agent.queue.BuildRunner
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.util.file.DefaultFileKUtils
import uk.q3c.util.file.FileKUtils
import uk.q3c.util.testutil.LogMonitor

import java.time.OffsetDateTime

import static org.mockito.Mockito.*

/**
 * Created by David Sowerby on 25 Jul 2017
 */
class DefaultBuildRecordWriterTest extends Specification {

    InstallationInfo installationInfo = mock(InstallationInfo)
    BuildRecordCollator buildRecordCollator = mock(BuildRecordCollator)
    Build build = mock(Build)
    BuildRunner buildRunner = mock(BuildRunner)
    FileKUtils fileUtils
    BuildRecordWriter writer

    @Rule
    TemporaryFolder temporaryFolder
    File temp
    File buildDir
    UUID uid
    BuildRecord record
    File recordFile

    def setup() {
        uid = UUID.randomUUID()
        record = new BuildRecord(uid, OffsetDateTime.now(), false)

        temp = temporaryFolder.getRoot()
        buildDir = new File(temp, "wiggly")
        recordFile = new File(buildDir, "buildRecord.json")

        when(installationInfo.buildOutputDir(build)).thenReturn(buildDir)
        when(build.buildRunner).thenReturn(buildRunner)
        when(buildRunner.uid).thenReturn(uid)
        when(buildRecordCollator.getRecord(uid)).thenReturn(record)
    }

    def "folder exists"() {
        given:
        fileUtils = new DefaultFileKUtils()
        writer = new DefaultBuildRecordWriter(installationInfo, new ObjectMapper(), buildRecordCollator, fileUtils)
        if (!buildDir.exists()) {
            FileUtils.forceMkdir(buildDir)
        }

        when:
        writer.write(build)

        then:
        recordFile.exists()
    }

    def "folder does not exist"() {
        given:
        fileUtils = new DefaultFileKUtils()
        writer = new DefaultBuildRecordWriter(installationInfo, new ObjectMapper(), buildRecordCollator, fileUtils)
        if (buildDir.exists()) {
            FileUtils.forceDelete(buildDir)
        }

        when:
        writer.write(build)

        then:
        recordFile.exists()
    }

    def "folder does not exist and cannot be created"() {
        given:
        LogMonitor logMonitor = new LogMonitor()
        logMonitor.addClassFilter(DefaultBuildRecordWriter)
        fileUtils = mock(FileKUtils)
        when(fileUtils.forceMkdir(buildDir)).thenThrow(new RuntimeException("fake"))
        writer = new DefaultBuildRecordWriter(installationInfo, new ObjectMapper(), buildRecordCollator, fileUtils)
        if (buildDir.exists()) {
            FileUtils.forceDelete(buildDir)
        }

        when:
        writer.write(build)

        then:
        !recordFile.exists()
        !logMonitor.errorLogs().isEmpty()
    }
}
