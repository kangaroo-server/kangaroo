/*
 * Copyright (c) 2017 Michael Krotscheck
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

import org.apache.commons.configuration.Configuration;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * A custom, configurable binder that may be used to inject additional
 * configuration instances into the system. These will be picked up by
 * the SystemConfiguration injectee.
 *
 * @author Michael Krotscheck
 */
public final class ConfigurationBinder extends AbstractBinder {

    /**
     * The configuration to inject.
     */
    private final Configuration config;

    /**
     * Create a new binder around a specific configuration instance.
     *
     * @param config The configuration to wrap.
     */
    public ConfigurationBinder(final Configuration config) {
        this.config = config;
    }

    /**
     * Inject this configuration into the context.
     */
    @Override
    protected void configure() {
        bind(config)
                .to(Configuration.class)
                .named("kangaroo_external_configuration");
    }
}
