package uk.q3c.kaytee.agent.build

import java.util.*

/**
 * Created by David Sowerby on 25 Mar 2017
 */
interface BuildRecordCollator {
    val records: MutableMap<UUID, BuildRecord>
    fun getRecord(buildRequestId: UUID): BuildRecord
}