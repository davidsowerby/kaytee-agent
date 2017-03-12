package uk.q3c.simplycd.agent.build

/**
 * Created by David Sowerby on 14 Jan 2017
 */
interface BuildNumberReader {

    fun nextBuildNumber(projectName: String): Int
}