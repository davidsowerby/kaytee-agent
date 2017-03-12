package uk.q3c.simplycd.agent.build

import com.google.inject.Inject
import org.jetbrains.annotations.NotNull
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.agent.queue.ManualTaskLauncher
import uk.q3c.simplycd.agent.queue.ManualTaskRequest
import uk.q3c.simplycd.agent.queue.TaskCompletedMessage
import uk.q3c.simplycd.agent.queue.TaskRequest
import uk.q3c.simplycd.i18n.BuildResultStateKey

import java.time.LocalDateTime

/**
 * Created by David Sowerby on 30 Jan 2017
 */
class MockManualTaskLauncher implements ManualTaskLauncher {

    private GlobalBusProvider globalBusProvider

    static class ManualTaskService implements Runnable {

        GlobalBusProvider globalBusProvider
        TaskRequest taskRequest

        ManualTaskService(GlobalBusProvider globalBusProvider, TaskRequest taskRequest) {
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
            BuildResultStateKey resultStateKey = randomiser.fail ? BuildResultStateKey.Build_Failure : BuildResultStateKey.Build_Successful
            TaskCompletedMessage msg = new TaskCompletedMessage(taskRequest, start, end, resultStateKey)
            globalBusProvider.get().publish(msg)
            println "Manual task completion message sent: $resultStateKey "
        }
    }

    @Inject
    MockManualTaskLauncher(GlobalBusProvider globalBusProvider) {
        this.globalBusProvider = globalBusProvider
    }

    @Override
    void run(@NotNull ManualTaskRequest taskRequest) {

        new Thread(new ManualTaskService(globalBusProvider, taskRequest)).run()

    }
}
