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

package net.krotscheck.kangaroo.authz.oauth2;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorFeature;
import net.krotscheck.kangaroo.authz.common.cors.AuthzCORSFeature;
import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.authz.oauth2.exception.RedirectingExceptionMapper;
import net.krotscheck.kangaroo.authz.oauth2.factory.CredentialsFactory;
import net.krotscheck.kangaroo.authz.oauth2.filter.ClientAuthorizationFilter;
import net.krotscheck.kangaroo.authz.oauth2.resource.AuthorizationService;
import net.krotscheck.kangaroo.authz.oauth2.resource.TokenService;
import net.krotscheck.kangaroo.authz.oauth2.resource.authorize.AuthCodeHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.authorize.ImplicitHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.AuthorizationCodeGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.ClientCredentialsGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.OwnerCredentialsGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.resource.token.RefreshTokenGrantHandler;
import net.krotscheck.kangaroo.authz.oauth2.tasks.TokenCleanupTask;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.common.logging.LoggingFeature;
import net.krotscheck.kangaroo.common.security.SecurityFeature;
import net.krotscheck.kangaroo.common.status.StatusFeature;
import net.krotscheck.kangaroo.common.timedtasks.TimedTasksFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

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
        property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);

        // Common features.
        register(LoggingFeature.class);          // Logging Configuration
        register(ConfigurationFeature.class);    // Configuration loader
        register(JacksonFeature.class);          // Data Type de/serialization.
        register(ExceptionFeature.class);        // Exception Mapping.
        register(DatabaseFeature.class);         // Database Feature.
        register(StatusFeature.class);           // Heartbeat service.
        register(SecurityFeature.class);         // Security components.
        register(TimedTasksFeature.class);       // Timed tasks service.
        register(AuthenticatorFeature.class);    // OAuth2 Authenticators
        register(AuthzCORSFeature.class);        // CORS feature.

        // Asset factories
        register(new CredentialsFactory.Binder());

        // Service filters
        register(new ClientAuthorizationFilter.Binder());

        // Authorization handlers
        register(new AuthCodeHandler.Binder());
        register(new ImplicitHandler.Binder());

        // ResponseType and GrantType handlers
        register(new ClientCredentialsGrantHandler.Binder());
        register(new RefreshTokenGrantHandler.Binder());
        register(new AuthorizationCodeGrantHandler.Binder());
        register(new OwnerCredentialsGrantHandler.Binder());

        // Timed tasks
        register(new TokenCleanupTask.Binder()); // Cleanup old tokens.

        // Exception Mappers
        register(new RedirectingExceptionMapper.Binder());

        // Resource services
        register(TokenService.class);
        register(AuthorizationService.class);
    }
}
