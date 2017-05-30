package uk.q3c.kaytee.agent.build

/**
 * Created by David Sowerby on 14 Jan 2017
 */
interface BuildNumberReader {

    fun nextBuildNumber(build: Build): String
}