package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.queue.ManualTaskRunner
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockManualTaskRunner implements ManualTaskRunner {
    Build build
    TaskKey taskKey

    MockManualTaskRunner(Build build, TaskKey taskKey) {
        this.build = build
        this.taskKey = taskKey
    }

    @Override
    String identity() {
        return "Mock Manual task runner for $taskKey, includeQualityGate is: $includeQualityGate"
    }

    @Override
    void run() {

    }
}
