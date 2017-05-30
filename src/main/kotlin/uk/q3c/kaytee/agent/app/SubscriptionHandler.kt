package uk.q3c.kaytee.agent.app

import com.google.common.collect.ImmutableList
import com.google.inject.Singleton
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.http.HttpMethod.DELETE
import ratpack.http.HttpMethod.POST
import ratpack.jackson.Jackson
import uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey
import javax.inject.Inject

/**
 * Created by David Sowerby on 13 Mar 2017
 */
@Singleton class SubscriptionHandler @Inject constructor(val hooks: Hooks, errorResponseBuilder: ErrorResponseBuilder) : AbstractHandler(errorResponseBuilder) {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    init {
        validMethodCalls = ImmutableList.of(POST, DELETE)
        uri = subscriptions
    }

    /**
     * Unsubscribe
     */
    override fun delete(context: Context) {
        context.parse(Jackson.fromJson(SubscriptionRequest::class.java))
                .then { subscriptionRequest ->
                    log.debug("removing subscription request for topic '{}' to call back '{}'", subscriptionRequest.topicUrl, subscriptionRequest.callbackUrl)

                    if (hooks.unsubscribe(subscriptionRequest.topicUrl, subscriptionRequest.callbackUrl)) {
                        log.debug("subscription removed")
                        context.response.status(HttpStatus.SC_OK)
                        context.render("OK")
                    } else {
                        log.debug("subscription not found")
                        context.response.status(HttpStatus.SC_OK)
                        context.render("OK Subscription did not exist")
                    }
                }
    }

    override fun post(context: Context) {
        context.parse(Jackson.fromJson(SubscriptionRequest::class.java))
                .then { subscriptionRequest ->
                    log.debug("processing subscription request for topic '{}' to call back '{}'", subscriptionRequest.topicUrl, subscriptionRequest.callbackUrl)

                    if (hooks.subscribe(subscriptionRequest.topicUrl, subscriptionRequest.callbackUrl)) {
                        log.debug("subscription successful")
                        context.response.status(HttpStatus.SC_OK)
                        context.render("OK")
                    } else {
                        log.debug("subscription failed - has the topic been registered?")
                        val errorResponse = errorResponseBuilder.build(uri, DeveloperErrorMessageKey.Invalid_Topic, subscriptionRequest.topicUrl)
                        context.response.status(DeveloperErrorMessageKey.Invalid_Topic.httpCode)
                        context.render(Jackson.json(errorResponse))
                    }
                }
    }
}