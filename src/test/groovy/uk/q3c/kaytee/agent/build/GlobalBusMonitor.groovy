package uk.q3c.kaytee.agent.build

import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import uk.q3c.kaytee.agent.eventbus.GlobalBus
import uk.q3c.kaytee.agent.eventbus.SubscribeTo
import uk.q3c.kaytee.agent.queue.BuildMessage

/**
 * Created by David Sowerby on 03 Jun 2017
 */

@Listener
@SubscribeTo(GlobalBus.class)
class GlobalBusMonitor {

    Map<UUID, List<BuildMessage>> messages = new HashMap()
    final Object lock = new Object()

    @Handler
    void monitor(BuildMessage busMessage) {
        synchronized (lock) {
            List<BuildMessage> msgs = messages.get(busMessage.buildRequestId)
            busMessage.buildRequestId
            if (msgs == null) {
                msgs = new ArrayList<>()
                messages.put(busMessage.buildRequestId, msgs)
            }
            msgs.add(busMessage)
        }
    }

}
