package uk.q3c.simplycd.agent.app

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import ratpack.handling.HandlerDecorator
import uk.q3c.simplycd.agent.queue.BuildRequestHandler

/**
 * An example Guice module.
 */
class ApiModule : AbstractModule() {
    /**
     * Adds a service impl to the application, and registers a decorator so that all requests are logged.
     * Registered implementations of {@link ratpack.handling.HandlerDecorator} are able to decorate the
     * application handler.
     *
     * @see RootHandler
     */
    override fun configure() {
        bind(RootHandler::class.java)
        bind(BuildRequestHandler::class.java)
        bind(ErrorResponseBuilder::class.java).to(DefaultErrorResponseBuilder::class.java)
        bind(TopicRegistrar::class.java).to(DefaultTopicRegistrar::class.java)
        bind(SubscriptionHandler::class.java)
        bind(Hooks::class.java).to(DefaultHooks::class.java).`in`(Scopes.SINGLETON)
        bind(SubscriberNotifier::class.java).to(DefaultSubscriberNotifier::class.java)
        Multibinder.newSetBinder(binder(), HandlerDecorator::class.java)
                .addBinding()
                .toInstance(ratpack.handling.HandlerDecorator.prepend(LoggingHandler()))
    }
}
