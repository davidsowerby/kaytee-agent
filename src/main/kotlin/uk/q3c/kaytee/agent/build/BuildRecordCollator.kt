package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.i18n.BuildStateKey
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
     * A map containing a count of states of builds managed by this collator.  Note that [updateBuildStateCount] MUST be called
     * before accessing this property if an up to date count is required.
     */
    val buildStateCount: MutableMap<BuildStateKey, Int>

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

    /**
     * Update [buildStateCount].  Use sparingly, as the build records are locked while the count is made
     */
    fun updateBuildStateCount()

    fun hasRecord(uid: UUID): Boolean
}