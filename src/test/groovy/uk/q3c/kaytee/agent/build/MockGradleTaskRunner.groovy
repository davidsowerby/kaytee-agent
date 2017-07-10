package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.queue.GradleTaskRunner
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockGradleTaskRunner implements GradleTaskRunner {
    Build build
    TaskKey taskKey
    boolean includeQualityGate

    MockGradleTaskRunner(Build build, TaskKey taskKey, boolean includeQualityGate) {
        this.build = build
        this.taskKey = taskKey
        this.includeQualityGate = includeQualityGate
    }

    @Override
    String identity() {
        return "Mock Gradle task runner for $taskKey, includeQualityGate is: $includeQualityGate"
    }

    @Override
    void run() {

    }
}
