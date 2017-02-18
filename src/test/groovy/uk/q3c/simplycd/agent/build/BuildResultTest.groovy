package uk.q3c.simplycd.agent.build

import spock.lang.Specification
import uk.q3c.simplycd.build.BuildResult
import uk.q3c.simplycd.i18n.BuildResultStateKey
import uk.q3c.simplycd.queue.QueueRequest

import java.time.LocalDateTime

/**
 * Created by David Sowerby on 15 Jan 2017
 */
class BuildResultTest extends Specification {

    QueueRequest queueRequest = Mock(QueueRequest)

    def "pass and fail"() {
        given:
        LocalDateTime start = LocalDateTime.now()
        LocalDateTime end = LocalDateTime.now()

        expect:
        new BuildResult(queueRequest, start, end, BuildResultStateKey.Unsupported_Gradle_Version).failed()
        new BuildResult(queueRequest, start, end, BuildResultStateKey.Unsupported_Build_Argument).failed()
        new BuildResult(queueRequest, start, end, BuildResultStateKey.Build_Successful).passed()
    }
}
