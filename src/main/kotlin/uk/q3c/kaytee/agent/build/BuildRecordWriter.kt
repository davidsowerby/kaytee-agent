package uk.q3c.kaytee.agent.build

/**
 * Created by David Sowerby on 19 Jul 2017
 */
interface BuildRecordWriter {
    fun write(build: Build)
}