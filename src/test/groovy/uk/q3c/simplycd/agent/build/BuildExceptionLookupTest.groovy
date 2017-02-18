package uk.q3c.simplycd.agent.build

import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException
import spock.lang.Specification
import uk.q3c.simplycd.build.BuildExceptionLookup
import uk.q3c.simplycd.build.BuildPreparationException

import static uk.q3c.simplycd.i18n.BuildResultStateKey.*

/**
 * Created by David Sowerby on 15 Jan 2017
 */
class BuildExceptionLookupTest extends Specification {

    BuildExceptionLookup lookup

    def setup() {
        lookup = new BuildExceptionLookup()
    }

    def "convert"() {

        expect:
//        lookup.lookupKeyFromException(new UnsupportedVersionException("x")) == Unsupported_Gradle_Version
        lookup.lookupKeyFromException(new UnsupportedOperationConfigurationException("x")) == Unsupported_Operation_Configuration
        lookup.lookupKeyFromException(new UnsupportedBuildArgumentException("x")) == Unsupported_Build_Argument
        lookup.lookupKeyFromException(new BuildException("x", new NullPointerException())) == Build_Failure
        lookup.lookupKeyFromException(new BuildCancelledException("x")) == Build_Cancelled
        lookup.lookupKeyFromException(new GradleConnectionException("x")) == Gradle_Connection_Failure
        lookup.lookupKeyFromException(new IllegalStateException("x")) == Gradle_Illegal_State
        lookup.lookupKeyFromException(new NullPointerException("x")) == Unexpected_Exception_Type
        lookup.lookupKeyFromException(new BuildPreparationException("x", new NullPointerException())) == Preparation_Failed
    }
}
