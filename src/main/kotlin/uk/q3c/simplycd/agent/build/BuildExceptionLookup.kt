package uk.q3c.simplycd.agent.build

import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException
import org.slf4j.LoggerFactory
import uk.q3c.simplycd.agent.i18n.BuildFailCauseKey
import uk.q3c.simplycd.agent.i18n.BuildFailCauseKey.*

/**
 * Created by David Sowerby on 15 Jan 2017
 */
class BuildExceptionLookup {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    fun lookupKeyFromException(e: Exception): BuildFailCauseKey {
        return when (e) {
        // these two must be in this order to allow for inheritance
            is UnsupportedOperationConfigurationException -> Unsupported_Operation_Configuration
            is UnsupportedVersionException -> Unsupported_Gradle_Version

            is BuildException -> Build_Failed
            is BuildCancelledException -> Build_Cancelled
            is BuildConfigurationException -> Build_Configuration

        // these two must be in this order to allow for inheritance
            is UnsupportedBuildArgumentException -> Unsupported_Build_Argument
            is GradleConnectionException -> Gradle_Connection_Failure

            is IllegalStateException -> Gradle_Illegal_State
            is BuildPreparationException -> Preparation_Failed
            is TaskException -> Task_Failure
            else -> {
                log.warn("Unexpected exception type causing Build failure: ", e)
                Unexpected_Exception_Type
            }
        }
    }
}