package uk.q3c.simplycd.agent.api

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import ratpack.handling.HandlerDecorator

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
        bind(ErrorResponseBuilder::class.java).to(DefaultErrorResponseBuilder::class.java)
        Multibinder.newSetBinder(binder(), HandlerDecorator::class.java)
                .addBinding()
                .toInstance(ratpack.handling.HandlerDecorator.prepend(LoggingHandler()))
    }
}
