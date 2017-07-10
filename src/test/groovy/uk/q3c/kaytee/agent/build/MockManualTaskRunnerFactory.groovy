package uk.q3c.kaytee.agent.build

import org.jetbrains.annotations.NotNull
import uk.q3c.kaytee.agent.queue.ManualTaskRunner
import uk.q3c.kaytee.agent.queue.ManualTaskRunnerFactory
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockManualTaskRunnerFactory implements ManualTaskRunnerFactory {
    Map<TaskKey, MockManualTaskRunner> runners = new HashMap<>()


    @Override
    ManualTaskRunner create(@NotNull Build build, @NotNull TaskKey taskKey) {
        ManualTaskRunner runner = new MockManualTaskRunner(build, taskKey)
        runners.put(taskKey, runner)
        return runner
    }
}
