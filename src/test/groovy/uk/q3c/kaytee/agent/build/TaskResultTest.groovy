package uk.q3c.kaytee.agent.build

import spock.lang.Specification
import spock.lang.Unroll
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Created by David Sowerby on 25 Jun 2017
 */
class TaskResultTest extends Specification {

    TaskResult taskResult

    def setup() {
        taskResult = new TaskResult(TaskKey.Unit_Test)
    }

    @Unroll
    def "hasCompleted"() {
        when:
        taskResult.state = state

        then:
        taskResult.hasCompleted() == expCompleted

        where:
        state                            | expCompleted
        TaskStateKey.Not_Run             | false
        TaskStateKey.Started             | false
        TaskStateKey.Requested           | false
        TaskStateKey.Not_Required        | true
        TaskStateKey.Successful          | true
        TaskStateKey.Quality_Gate_Failed | true
        TaskStateKey.Failed              | true
        TaskStateKey.Cancelled           | true
    }
}
