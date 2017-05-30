package uk.q3c.kaytee.agent.build

import com.google.inject.Inject
import org.jetbrains.annotations.NotNull
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.kaytee.agent.queue.ManualTaskLauncher
import uk.q3c.kaytee.agent.queue.ManualTaskRunner
import uk.q3c.kaytee.agent.queue.TaskRunner
import uk.q3c.kaytee.agent.queue.TaskSuccessfulMessage

import java.time.LocalDateTime
/**
 * Created by David Sowerby on 30 Jan 2017
 */
class MockManualTaskLauncher implements ManualTaskLauncher {

    private GlobalBusProvider globalBusProvider

    static class ManualTaskService implements Runnable {

        GlobalBusProvider globalBusProvider
        TaskRunner taskRequest

        ManualTaskService(GlobalBusProvider globalBusProvider, TaskRunner taskRequest) {
            this.globalBusProvider = globalBusProvider
            this.taskRequest = taskRequest
        }

        @Override
        void run() {
            LocalDateTime start = LocalDateTime.now()
            TaskRandomiser randomiser = new TaskRandomiser()
            randomiser.calculate()

            int d = randomiser.duration
            println "MANUAL TASK RUNNING for ${d} ms"
            while (d > 0) {
                d--
            }
            LocalDateTime end = LocalDateTime.now()
            BuildStateKey resultStateKey = randomiser.fail ? BuildStateKey.Failed : BuildStateKey.Successful
            TaskSuccessfulMessage msg = new TaskSuccessfulMessage(taskRequest.build.buildRunner.uid, taskRequest.taskKey)
            globalBusProvider.get().publish(msg)
            println "Manual task completion message sent: $resultStateKey "
        }
    }

    @Inject
    MockManualTaskLauncher(GlobalBusProvider globalBusProvider) {
        this.globalBusProvider = globalBusProvider
    }

    @Override
    void run(@NotNull ManualTaskRunner taskRequest) {

        new Thread(new ManualTaskService(globalBusProvider, taskRequest)).run()

    }
}
