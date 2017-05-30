/*
 *
 *  * Copyright (c) 2016. David Sowerby
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package uk.q3c.kaytee.agent.eventbus;

import com.google.inject.*;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import net.engio.mbassy.bus.AbstractPubSubSupport;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.SyncMessageBus;
import net.engio.mbassy.bus.common.Properties;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.ConfigurationErrorHandler;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configures Event bus implementations for Singleton scope only.  All classes annotated with {@link Listener} are subscribed to the bus
 * <p>
 * Created by David Sowerby on 08/03/15.
 */
public class GlobalBusModule extends AbstractModule {
    public final static String BUS_SCOPE = "bus_id";
    public final static String BUS_INDEX = "bus_index";
    private static Logger log = LoggerFactory.getLogger(GlobalBusModule.class);
    private AtomicInteger globalBusIndex = new AtomicInteger(1);

    /**
     * Configures a {@link Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        TypeLiteral<PubSubSupport<BusMessage>> eventBusLiteral = new TypeLiteral<PubSubSupport<BusMessage>>() {
        };

        Key<PubSubSupport<BusMessage>> globalBusKey = Key.get(eventBusLiteral, GlobalBus.class);
        final Provider<PubSubSupport<BusMessage>> globalBusProvider = this.getProvider(globalBusKey);


        bindListener(new ListenerAnnotationMatcher(), new BusTypeListener(globalBusProvider));
        bindConfigurationErrorHandlers();
        bindPublicationErrorHandlers();
        bindBusProviders();
    }

    /**
     * Use bus providers where you want to enforce the use of a particular bus by sub-classes.  This avoids an annotated
     * constructor parameter in a super-class being ignored / overridden in a sub-class
     */
    protected void bindBusProviders() {
        bind(GlobalBusProvider.class).to(DefaultGlobalBusProvider.class);
    }


    /**
     * All buses use the default error handler by default.  Override this method to provide alternative bindings.
     */
    protected void bindConfigurationErrorHandlers() {
        bind(ConfigurationErrorHandler.class).annotatedWith(uk.q3c.kaytee.agent.eventbus.GlobalBus.class)
                .to(DefaultEventBusConfigurationErrorHandler.class);
    }

    /**
     * All buses use the default error handler by default.  Override this method to provide alternative bindings.
     */
    protected void bindPublicationErrorHandlers() {
        bind((IPublicationErrorHandler.class)).annotatedWith(uk.q3c.kaytee.agent.eventbus.GlobalBus.class)
                .to(DefaultEventBusErrorHandler.class);
    }


    @Provides
    protected EventBusAutoSubscriber autoSubscriber(@GlobalBus Provider<PubSubSupport<BusMessage>> globalBus) {
        return new DefaultEventBusAutoSubscriber(globalBus);
    }


    /**
     * Refer to the MBassador documentation at https://github.com/bennidi/mbassador/wiki/Configuration for more
     * information about the configuration itself.
     *
     * @return configuration for the GlobalBus
     */
    @Provides
    @GlobalBus
    protected IBusConfiguration globalBusConfig() {
        return new BusConfiguration().addFeature(Feature.SyncPubSub.Default())
                .addFeature(Feature.AsynchronousHandlerInvocation.Default())
                .addFeature(Feature.AsynchronousMessageDispatch.Default());

    }


    private PubSubSupport<BusMessage> createBus(IBusConfiguration config, IPublicationErrorHandler publicationErrorHandler, ConfigurationErrorHandler
            configurationErrorHandler, String name, boolean useAsync) {
        config.setProperty(Properties.Handler.PublicationError, publicationErrorHandler);
        config.addConfigurationErrorHandler(configurationErrorHandler);
        PubSubSupport<BusMessage> eventBus;
        eventBus = (useAsync) ? new MBassador<>(config) : new SyncMessageBus<>(config);
        ((AbstractPubSubSupport) eventBus).addErrorHandler(publicationErrorHandler);
        log.debug("instantiated a {} Bus with id {}", name, eventBus.getRuntime()
                .get(Properties.Common.Id));
        return eventBus;
    }


    @Provides
    @GlobalBus
    @Singleton
    protected PubSubSupport<BusMessage> providesGlobalBus(@GlobalBus IBusConfiguration config, @GlobalBus IPublicationErrorHandler publicationErrorHandler,
                                                          @GlobalBus ConfigurationErrorHandler configurationErrorHandler) {
        PubSubSupport<BusMessage> bus = createBus(config, publicationErrorHandler, configurationErrorHandler, "Global", true);
        bus.getRuntime()
                .add(BUS_SCOPE, "global")
                .add(BUS_INDEX, globalBusIndex.getAndIncrement());
        return bus;
    }

    /**
     * Matches classes annotated with {@link Listener}
     */
    private static class ListenerAnnotationMatcher extends AbstractMatcher<TypeLiteral<?>> {
        @Override
        public boolean matches(TypeLiteral<?> t) {
            Class<?> rawType = t.getRawType();
            return rawType.isAnnotationPresent(Listener.class);
        }
    }

    private static class BusTypeListener implements TypeListener {
        private Provider<PubSubSupport<BusMessage>> globalBusProvider;

        public BusTypeListener(Provider<PubSubSupport<BusMessage>> globalBusProvider) {
            this.globalBusProvider = globalBusProvider;
        }

        /**
         * The logic for auto subscribing can be changed by providing an alternative implementation of
         * EventBusAutoSubscriber, but it has to be created using 'new' here, because the Injector is not yet complete
         *
         * @param type
         * @param encounter
         * @param <I>
         */
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            encounter.register(new DefaultEventBusAutoSubscriber(globalBusProvider));
        }
    }
}
