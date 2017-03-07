/*
 * Copyright (c) 2015. David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.q3c.simplycd.agent.eventbus;

import com.google.inject.Provider;
import com.google.inject.spi.InjectionListener;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.listener.Listener;

import java.lang.annotation.Annotation;

/**
 * Provides logic for automatically subscribing to event buses.  This is used as an {@link InjectionListener}, and cannot therefore use injection in its
 * constructor
 * <p>
 * Created by David Sowerby on 13/03/15.
 */
public class DefaultEventBusAutoSubscriber implements EventBusAutoSubscriber {


    private Provider<PubSubSupport<BusMessage>> globalBusProvider;

    public DefaultEventBusAutoSubscriber(Provider<PubSubSupport<BusMessage>> globalBusProvider) {
        this.globalBusProvider = globalBusProvider;
    }

    /**
     * Invoked by Guice after it injects the fields and methods of instance.  {@code injectee} must have a {@link Listener} annotation in order to get this
     * far (the matcher will only select those which have).
     * <p>
     * Subscribes singleton objects to the Global Bus
     *
     * @param injectee instance that Guice injected dependencies into
     */
    @Override
    public void afterInjection(Object injectee) {
        Class<?> clazz = injectee.getClass();
        SubscribeTo subscribeTo = clazz.getAnnotation(SubscribeTo.class);
        if (subscribeTo == null) { //default behaviour
            globalBusProvider.get()
                    .subscribe(injectee);
            return;
        } else { //defined by SubscribeTo
            Class<? extends Annotation>[] targets = subscribeTo.value();
            for (Class<? extends Annotation> target : targets) {
                if (target.equals(GlobalBus.class)) {
                    globalBusProvider.get()
                            .subscribe(injectee);
                }


            }
        }

    }
}
