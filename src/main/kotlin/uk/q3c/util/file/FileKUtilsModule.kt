package uk.q3c.util.file

import com.google.inject.AbstractModule

/**
 * Created by David Sowerby on 21 Jul 2017
 */
class FileKUtilsModule : AbstractModule() {


    override fun configure() {
        bind(FileKUtils::class.java).to(DefaultFileKUtils::class.java)
    }
}