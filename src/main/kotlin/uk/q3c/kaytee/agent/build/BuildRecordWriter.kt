package uk.q3c.kaytee.agent.build

/**
 *
 * Writes [BuildRecord] and stacktrace to the build-output folder
 *
 * Created by David Sowerby on 19 Jul 2017
 */
interface BuildRecordWriter {
    fun write(build: Build)
}