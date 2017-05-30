package uk.q3c.kaytee.agent.system

import uk.q3c.kaytee.agent.i18n.MessageKey

/**
 * Notifies registered web hooks of various events
 *
 * Created by David Sowerby on 07 Mar 2017
 */
interface RestNotifier {
    fun notifyInformation(key: MessageKey, msg: String)
}