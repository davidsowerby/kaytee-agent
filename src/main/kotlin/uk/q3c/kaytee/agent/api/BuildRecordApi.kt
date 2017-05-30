package uk.q3c.kaytee.agent.api

import uk.q3c.kaytee.agent.app.buildRecords
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.rest.hal.HalResource

/**
 * Created by David Sowerby on 04 Apr 2017
 */

class BuildRecordList(val records: List<BuildRecord>) : HalResource() {
    init {
        self(buildRecords)
    }
}