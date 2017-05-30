package uk.q3c.kaytee.agent.build

import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException
import spock.lang.Specification
import uk.q3c.kaytee.agent.i18n.TaskResultStateKey

import static uk.q3c.kaytee.agent.i18n.BuildFailCauseKey.*
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
        lookup.lookupKeyFromException(new TaskException(TaskResultStateKey.Task_Cancelled)) == Task_Failure
        lookup.lookupKeyFromException(new UnsupportedBuildArgumentException("x")) == Unsupported_Build_Argument
        lookup.lookupKeyFromException(new BuildException("x", new NullPointerException())) == Build_Failed
        lookup.lookupKeyFromException(new BuildCancelledException("x")) == Build_Cancelled
        lookup.lookupKeyFromException(new GradleConnectionException("x")) == Gradle_Connection_Failure
        lookup.lookupKeyFromException(new IllegalStateException("x")) == Gradle_Illegal_State
        lookup.lookupKeyFromException(new NullPointerException("x")) == Unexpected_Exception_Type
        lookup.lookupKeyFromException(new BuildPreparationException("x", new NullPointerException())) == Preparation_Failed
    }
}
