package uk.q3c.kaytee.agent.build

import org.jetbrains.annotations.NotNull
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.queue.GradleTaskRunner
import uk.q3c.kaytee.agent.queue.GradleTaskRunnerFactory
import uk.q3c.kaytee.plugin.TaskKey
/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockGradleTaskRunnerFactory implements GradleTaskRunnerFactory {
    Map<TaskKey, MockGradleTaskRunner> runners = new HashMap<>()
    GlobalBusProvider globalBusProvider
    TaskKey failingTask = null

    @Override
    GradleTaskRunner create(@NotNull Build build, @NotNull TaskKey taskKey, boolean includeQualityGate) {
        MockGradleTaskRunner runner = new MockGradleTaskRunner(globalBusProvider, build, taskKey, includeQualityGate)
        if (taskKey == failingTask) {
            runner.failOnRun = true
        }
        runners.put(taskKey, runner)
        return runner
    }
}
