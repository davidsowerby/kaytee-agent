package uk.q3c.simplycd.agent.build

import java.util.*

/**
 * Created by David Sowerby on 25 Mar 2017
 */
interface BuildResultCollator {
    val results: MutableMap<UUID, BuildResult>
    fun getResult(buildRequestId: UUID): BuildResult
}