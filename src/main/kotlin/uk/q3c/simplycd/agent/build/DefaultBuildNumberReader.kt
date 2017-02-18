package uk.q3c.simplycd.build

import com.google.inject.Inject
import com.google.inject.Singleton

/**
 * Created by David Sowerby on 16 Jan 2017
 */
@Singleton
class DefaultBuildNumberReader @Inject constructor() : BuildNumberReader {

    override fun nextBuildNumber(projectName: String): Int {
        TODO()
    }
}