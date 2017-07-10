package uk.q3c.kaytee.agent.build

import org.jetbrains.annotations.NotNull
import uk.q3c.kaytee.agent.queue.GradleTaskRunner
import uk.q3c.kaytee.agent.queue.GradleTaskRunnerFactory
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockGradleTaskRunnerFactory implements GradleTaskRunnerFactory {
    Map<TaskKey, MockGradleTaskRunner> runners = new HashMap<>()

    @Override
    GradleTaskRunner create(@NotNull Build build, @NotNull TaskKey taskKey, boolean includeQualityGate) {
        GradleTaskRunner runner = new MockGradleTaskRunner(build, taskKey, includeQualityGate)
        runners.put(taskKey, runner)
        return runner
    }
}
