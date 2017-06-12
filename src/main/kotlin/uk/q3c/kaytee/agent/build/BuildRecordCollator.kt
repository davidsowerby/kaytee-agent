package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.queue.BuildMessage
import java.util.*

/**
 *
 *  Instances of implementations should only be used as a Singleton (currently defined that way in [BuildModule])
 *
 * All the messages generated during the build process are collated, with the resulting [BuildRecord] retained in memory
 * Future development will handle archiving the records
 *
 * Created by David Sowerby on 25 Mar 2017
 */
interface BuildRecordCollator {
    val records: MutableMap<UUID, BuildRecord>

    /**
     * Gets a record using the [buildMessage.buildRequestId] as a key.  If a record is not found, one is created
     */
    fun getRecord(buildMessage: BuildMessage): BuildRecord

    /**
     * Gets a record using [uid] as a key.  If a record is not found, an exception is thrown
     *
     * @throws InvalidBuildRequestIdException if the requested record is not found
     */
    fun getRecord(uid: UUID): BuildRecord
}