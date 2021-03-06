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

import net.engio.mbassy.bus.config.ConfigurationError;
import net.engio.mbassy.bus.config.ConfigurationErrorHandler;
import net.engio.mbassy.bus.config.IBusConfiguration;

/**
 * Responds to an MBassador configuration error by simply re-throwing the exception
 *
 * Created by David Sowerby on 10/03/15.
 */
public class DefaultEventBusConfigurationErrorHandler implements ConfigurationErrorHandler {
    /**
     * Called when a misconfiguration is detected on a {@link IBusConfiguration}
     *
     * @param error The error that represents the detected misconfiguration.
     */
    @Override
    public void handle(ConfigurationError error) {
        throw new EventBusException("Event bus configuration incorrect", error);
    }
}
