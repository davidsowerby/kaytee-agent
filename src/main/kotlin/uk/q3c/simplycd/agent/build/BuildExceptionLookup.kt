package uk.q3c.simplycd.build

import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException
import uk.q3c.simplycd.i18n.BuildResultStateKey

/**
 * Created by David Sowerby on 15 Jan 2017
 */
class BuildExceptionLookup {
    fun lookupKeyFromException(e: Exception): BuildResultStateKey {
        return when (e) {
        // these two must be in this order to allow for inheritance
            is UnsupportedOperationConfigurationException -> BuildResultStateKey.Unsupported_Operation_Configuration
            is UnsupportedVersionException -> BuildResultStateKey.Unsupported_Gradle_Version

            is BuildException -> BuildResultStateKey.Build_Failure
            is BuildCancelledException -> BuildResultStateKey.Build_Cancelled

        // these two must be in this order to allow for inheritance
            is UnsupportedBuildArgumentException -> BuildResultStateKey.Unsupported_Build_Argument
            is GradleConnectionException -> BuildResultStateKey.Gradle_Connection_Failure

            is IllegalStateException -> BuildResultStateKey.Gradle_Illegal_State
            is BuildPreparationException -> BuildResultStateKey.Preparation_Failed
            else ->
                BuildResultStateKey.Unexpected_Exception_Type
        }
    }
}