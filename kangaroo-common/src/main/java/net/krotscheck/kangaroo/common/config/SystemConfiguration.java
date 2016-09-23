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
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

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
     * @param context The application servlet context.
     */
    public SystemConfiguration(@Context final ServletContext context) {

        logger.debug("Adding system configuration");
        addConfiguration(
                new org.apache.commons.configuration.SystemConfiguration());

        try {
            logger.debug("Adding Servlet Context Metadata");
            String path = context.getRealPath("/META-INF/MANIFEST.MF");
            addConfiguration(new PropertiesConfiguration(path));
        } catch (ConfigurationException | NullPointerException ce) {
            logger.warn("Cannot load global properties file, does not exist "
                    + "or not readable.");
        }

        // Run the global configuration.
        configureGlobal();
    }

    /**
     * Return the current version of the system.
     *
     * @return A string representing the current version.
     */
    public String getVersion() {
        return getString("Implementation-Version", "dev");
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
        logger.info("Setting default timezone....");
        TimeZone.setDefault(getTimezone());
    }

    /**
     * HK2 Binder for our injector context.
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
