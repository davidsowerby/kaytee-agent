package uk.q3c.kaytee.agent.build

import spock.lang.Specification
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.plugin.TaskKey
import uk.q3c.rest.hal.HalMapper

import java.time.OffsetDateTime

/**
 * Created by David Sowerby on 18 Apr 2017
 */
class BuildRecordTest extends Specification {

    HalMapper halMapper
    BuildRecord buildRecord

    def setup() {
        halMapper = new HalMapper()
    }

    def "Lock properties are not serialised to Json"() {
        given:
        buildRecord = new BuildRecord(UUID.randomUUID(), OffsetDateTime.now(), false)
        StringWriter sw = new StringWriter()

        when:
        halMapper.writeValue(sw, buildRecord)
        String result = sw.toString()

        then:
        !result.contains("stateLock")
        !result.contains("taskLock")
    }

    def "hasCompleted returns true only when task results are complete"() {
        when:
        buildRecord = new BuildRecord(UUID.randomUUID(), OffsetDateTime.now(), false)
        buildRecord.state = BuildStateKey.Failed
        for (TaskKey taskKey : TaskKey.values()) {
            TaskResult taskResult = buildRecord.taskResult(taskKey)
            taskResult.state = TaskStateKey.Not_Required
        }

        then:
        buildRecord.hasCompleted()

        when:
        buildRecord.taskResult(TaskKey.Publish_to_Local).state = TaskStateKey.Started

        then:
        !buildRecord.hasCompleted()
    }
}
