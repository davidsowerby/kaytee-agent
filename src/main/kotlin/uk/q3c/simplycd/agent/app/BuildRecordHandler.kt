package uk.q3c.simplycd.agent.app

import com.google.inject.Inject
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.jackson.Jackson
import uk.q3c.simplycd.agent.build.BuildRecordService
import uk.q3c.simplycd.agent.build.rangeDefault
import uk.q3c.simplycd.agent.build.rangeFromDefault
import uk.q3c.simplycd.agent.build.unitDefault
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import java.util.*

/**
 * Created by David Sowerby on 01 Apr 2017
 */
class BuildRecordHandler @Inject constructor(errorResponseBuilder: ErrorResponseBuilder, val buildRecordService: BuildRecordService) : AbstractHandler(errorResponseBuilder) {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    init {
        uri = buildRecords
    }

    override fun get(context: Context) {
        log.debug("Processing GET")
        if (context.request.queryParams.containsKey(idProperty)) {
            val uid = context.request.queryParams[idProperty]
            if (uid != null) {
                getSelected(context, uid)
            } else {
                getList(context)
            }
        } else {
            getList(context)
        }

    }

    fun getList(context: Context) {
        val queryParams = context.request.queryParams

        val unitProperty = queryParams.getOrDefault(unitProperty, unitDefault)
        val rangeProperty = queryParams.getOrDefault(rangeProperty, rangeDefault.toString()).toInt()
        val rangeFromProperty = queryParams.getOrDefault(rangeFromProperty, rangeFromDefault.toString()).toInt()

        val recordList = buildRecordService.list(unitProperty, rangeProperty, rangeFromProperty)

        context.response.status(HttpStatus.SC_OK)
        context.render(Jackson.json(recordList))
    }

    private fun getSelected(context: Context, id: String) {
        try {
            val uid: UUID = UUID.fromString(id)
            val entry = buildRecordService.get(uid)
            context.response.status(HttpStatus.SC_OK)
            context.render(Jackson.json(entry))

        } catch (iae: IllegalArgumentException) {
            error(context, DeveloperErrorMessageKey.Unrecognised_Build_Record_Id, id)
        }


    }
}