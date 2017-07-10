package uk.q3c.kaytee.agent.build

import org.jetbrains.annotations.NotNull
import uk.q3c.kaytee.agent.queue.DelegatedProjectTaskRunner
import uk.q3c.kaytee.agent.queue.DelegatedProjectTaskRunnerFactory
import uk.q3c.kaytee.plugin.GroupConfig
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockDelegatedProjectTaskRunnerFactory implements DelegatedProjectTaskRunnerFactory {
    Map<TaskKey, MockDelegatedTaskRunner> runners = new HashMap<>()


    @Override
    DelegatedProjectTaskRunner create(
            @NotNull Build build, @NotNull TaskKey taskKey, @NotNull GroupConfig groupConfig) {
        DelegatedProjectTaskRunner runner = new MockDelegatedTaskRunner(build, taskKey, groupConfig)
        runners.put(taskKey, runner)
        return runner
    }
}
