package uk.q3c.kaytee.agent.build

import spock.lang.Specification
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


}
