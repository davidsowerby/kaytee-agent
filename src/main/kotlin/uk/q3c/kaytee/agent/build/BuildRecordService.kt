package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.api.BuildRecordList
import java.util.*

/**
 *
 *
 * Created by David Sowerby on 09 Apr 2017
 */
interface BuildRecordService {


    /**
     * Returns a list of [BuildRecord], determined by the parameters provided:
     *
     * @param unit "days", "weeks", "months" or "years", default [unitDefault]
     * @param range the number of [unit]s to return, default [rangeDefault]. Thus if [unit] is "days", results for 3 days will be returned
     * @param rangeFrom the [range] starts [rangeFrom] [unit] before now, default [rangeFromDefault] (now)
     *
     * So for example, with values of [unit]="weeks", [range]=3 and [rangeFrom]=1 would return results for a 3 week period ending 1 week before now
     */
    fun list(unit: String = unitDefault, range: Int = rangeDefault, rangeFrom: Int = rangeFromDefault): BuildRecordList

    /**
     * returns a [BuildRecord] for [uid]
     *
     * @throws IllegalArgumentException if the id is not recognised
     */
    fun get(uid: UUID): BuildRecord
}

class DefaultBuildRecordService : BuildRecordService {

    override fun get(uid: UUID): BuildRecord {
        TODO()
    }

    override fun list(unit: String, range: Int, rangeFrom: Int): BuildRecordList {
        TODO()
    }

}

val unitDefault: String = "days"
val rangeDefault: Int = 3
val rangeFromDefault = 0