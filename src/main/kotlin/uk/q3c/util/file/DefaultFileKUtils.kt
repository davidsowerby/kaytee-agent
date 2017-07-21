package uk.q3c.util.file

import java.io.File


/**
 *
 * A very thin wrapper on Apache [org.apache.commons.io.FileUtils], to allow mocking.  Methods have the same signature and just delegate to the static FileUtils calls
 *
 * Not all methods are implemented yet
 *
 * Created by David Sowerby on 21 Jul 2017
 */
interface FileKUtils {
    fun forceMkdir(file: File)
}

class DefaultFileKUtils : FileKUtils {

    override fun forceMkdir(file: File) {
        org.apache.commons.io.FileUtils.forceMkdir(file)
    }

}