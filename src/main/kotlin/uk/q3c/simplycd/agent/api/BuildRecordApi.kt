package uk.q3c.simplycd.agent.api

import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.app.buildRecords
import uk.q3c.simplycd.agent.build.BuildRecord

/**
 * Created by David Sowerby on 04 Apr 2017
 */

class BuildRecordList(val records: List<BuildRecord>) : HalResource() {
    init {
        self(buildRecords)
    }
}