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
 */

package net.krotscheck.oauth;


import net.krotscheck.features.config.ConfigurationFeature;
import net.krotscheck.features.database.DatabaseFeature;
import net.krotscheck.features.exception.ExceptionFeature;
import net.krotscheck.features.jackson.JacksonFeature;
import net.krotscheck.features.status.StatusFeature;
import net.krotscheck.features.user.UserFeature;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * The OID Servlet application, including all configured resources and
 * features.
 *
 * @author Michael Krotscheck
 */
public final class OAuthAPI extends ResourceConfig {

    /**
     * Constructor. Creates a new application instance.
     */
    public OAuthAPI() {
        // No autodiscovery, we load everything explicitly.
        property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);

        register(ConfigurationFeature.class);    // Configuration loader
        register(JacksonFeature.class);          // Data Type de/serialization.
        register(ExceptionFeature.class);        // Exception Mapping.
        register(DatabaseFeature.class);         // Database Feature.

        register(StatusFeature.class);           // Status service.
        register(UserFeature.class);             // User service.
    }
}
