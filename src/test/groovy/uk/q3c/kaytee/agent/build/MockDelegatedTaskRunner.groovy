package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.queue.DelegatedProjectTaskRunner
import uk.q3c.kaytee.plugin.GroupConfig
import uk.q3c.kaytee.plugin.TaskKey

/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockDelegatedTaskRunner implements DelegatedProjectTaskRunner {
    GroupConfig groupConfig
    Build build
    TaskKey taskKey

    MockDelegatedTaskRunner(Build build, TaskKey taskKey, GroupConfig groupConfig) {
        this.build = build
        this.taskKey = taskKey
        this.groupConfig = groupConfig
    }

    @Override
    String identity() {
        return "Mock delegated task runner for $taskKey"
    }

    @Override
    void run() {

    }


}
