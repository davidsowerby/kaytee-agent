package uk.q3c.simplycd.agent.build

import com.google.inject.Inject
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import uk.q3c.krail.core.eventbus.GlobalBus
import uk.q3c.krail.core.eventbus.SubscribeTo
import uk.q3c.simplycd.agent.queue.*

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by David Sowerby on 31 Jan 2017
 */
@Listener
@SubscribeTo(GlobalBus.class)
class QueueMessageReceiver {
    Queue<BuildRequestedMessage> buildRequests = new ConcurrentLinkedQueue<>()
    Queue<BuildStartedMessage> buildStarts = new ConcurrentLinkedQueue<>()
    Queue<BuildCompletedMessage> buildCompletions = new ConcurrentLinkedQueue<>()


    Queue<PreparationStartedMessage> preparationStarts = new ConcurrentLinkedQueue<>()
    Queue<PreparationCompletedMessage> preparationCompletions = new ConcurrentLinkedQueue<>()

    Queue<TaskStartedMessage> taskStarts = new ConcurrentLinkedQueue<>()
    Queue<TaskRequestedMessage> taskRequests = new ConcurrentLinkedQueue<>()
    Queue<TaskCompletedMessage> taskCompletions = new ConcurrentLinkedQueue<>()

    @Inject
    QueueMessageReceiver() {
    }

    @Handler
    void busMessage(BuildRequestedMessage busMessage) {
        buildRequests.add(busMessage)
    }

    @Handler
    void busMessage(TaskRequestedMessage busMessage) {
        taskRequests.add(busMessage)
    }

    @Handler
    void busMessage(TaskStartedMessage busMessage) {
        taskStarts.add(busMessage)
    }

    @Handler
    void busMessage(TaskCompletedMessage busMessage) {
        taskCompletions.add(busMessage)
    }

    @Handler
    void busMessage(BuildStartedMessage busMessage) {
        buildStarts.add(busMessage)
    }

    @Handler
    void busMessage(BuildCompletedMessage busMessage) {
        buildCompletions.add(busMessage)
    }

    @Handler
    void busMessage(PreparationStartedMessage busMessage) {
        preparationStarts.add(busMessage)
    }

    @Handler
    void busMessage(PreparationCompletedMessage busMessage) {
        preparationCompletions.add(busMessage)
    }

    boolean finishedBuilds() {
        return buildCompletions.size() == buildRequests.size()
    }
}

