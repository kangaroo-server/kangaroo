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

package net.krotscheck.kangaroo.servlet.oauth2;

import net.krotscheck.kangaroo.authenticator.PasswordAuthenticator;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.common.logging.LoggingFeature;
import net.krotscheck.kangaroo.common.status.StatusFeature;
import net.krotscheck.kangaroo.common.version.VersionFeature;
import net.krotscheck.kangaroo.database.DatabaseFeature;
import net.krotscheck.kangaroo.servlet.oauth2.factory.CredentialsFactory;
import net.krotscheck.kangaroo.servlet.oauth2.filter.ClientAuthorizationFilter;
import net.krotscheck.kangaroo.servlet.oauth2.resource.AuthorizationService;
import net.krotscheck.kangaroo.servlet.oauth2.resource.TokenService;
import net.krotscheck.kangaroo.servlet.oauth2.resource.grant.AuthorizationCodeGrantHandler;
import net.krotscheck.kangaroo.servlet.oauth2.resource.grant.ClientCredentialsGrantHandler;
import net.krotscheck.kangaroo.servlet.oauth2.resource.grant.OwnerCredentialsGrantHandler;
import net.krotscheck.kangaroo.servlet.oauth2.resource.grant.RefreshTokenGrantHandler;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * The OAuth API, hosted at /oauth.
 *
 * @author Michael Krotscheck
 */
public class OAuthAPI extends ResourceConfig {

    /**
     * Constructor. Creates a new application instance.
     */
    public OAuthAPI() {
        // No autodiscovery, we load everything explicitly.
        property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);

        // Common features.
        register(LoggingFeature.class);          // Logging Configuration
        register(ConfigurationFeature.class);    // Configuration loader
        register(JacksonFeature.class);          // Data Type de/serialization.
        register(ExceptionFeature.class);        // Exception Mapping.
        register(DatabaseFeature.class);         // Database Feature.
        register(VersionFeature.class);          // Version response attachment.
        register(StatusFeature.class);           // Heartbeat service.

        // Asset factories
        register(new CredentialsFactory.Binder());

        // Service filters
        register(new ClientAuthorizationFilter.Binder());

        // The password authenticator.
        register(new PasswordAuthenticator.Binder());

        // ResponseType and GrantType handlers
        register(new ClientCredentialsGrantHandler.Binder());
        register(new RefreshTokenGrantHandler.Binder());
        register(new AuthorizationCodeGrantHandler.Binder());
        register(new OwnerCredentialsGrantHandler.Binder());

        // Resource services
        register(TokenService.class);
        register(AuthorizationService.class);
    }
}
