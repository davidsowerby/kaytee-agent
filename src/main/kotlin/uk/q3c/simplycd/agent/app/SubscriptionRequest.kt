package uk.q3c.simplycd.agent.app

import uk.q3c.rest.hal.HalResource
import java.net.URL

/**
 * Created by David Sowerby on 15 Mar 2017
 */
class SubscriptionRequest(val topicUrl: URL, val callbackUrl: URL) : HalResource()