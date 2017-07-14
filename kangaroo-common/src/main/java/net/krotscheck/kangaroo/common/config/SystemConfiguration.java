/*
 * Copyright (c) 2016 Michael Krotscheck
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.krotscheck.kangaroo.common.config;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.TimeZone;

/**
 * Provides our configuration object, composite from various sources.
 *
 * @author Michael Krotscheck
 */
public final class SystemConfiguration extends CompositeConfiguration {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(SystemConfiguration.class);

    /**
     * Create an instance of the configuration provider.
     *
     * @param configurations Injected configuration.
     */
    @Inject
    public SystemConfiguration(
            @Named("kangaroo_external_configuration")
            final IterableProvider<Configuration> configurations) {

        // If the injected list of configurations is empty, at least add the
        // system configuration.
        logger.debug("Building System Configuration");
        if (configurations.getSize() == 0) {
            addConfiguration(
                    new org.apache.commons.configuration.SystemConfiguration());
        } else {
            configurations.forEach(this::addConfiguration);
        }

        // Run the global configuration.
        configureGlobal();
    }

    /**
     * Return the globally configured timezone.
     *
     * @return The configured timezone.
     */
    public TimeZone getTimezone() {
        return TimeZone.getTimeZone(getString("global.timezone", "UTC"));
    }

    /**
     * Run the global configuration settings.
     */
    private void configureGlobal() {
        logger.debug("Setting default timezone....");
        TimeZone.setDefault(getTimezone());
    }

    /**
     * HK2 ConfigurationBinder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(SystemConfiguration.class)
                    .to(SystemConfiguration.class)
                    .to(Configuration.class)
                    .named("system")
                    .in(Singleton.class);
        }
    }
}
