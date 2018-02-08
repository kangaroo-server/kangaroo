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

package net.krotscheck.kangaroo.authz.admin;


import io.swagger.annotations.Info;
import io.swagger.annotations.OAuth2Definition;
import io.swagger.annotations.OAuth2Definition.Flow;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import net.krotscheck.kangaroo.authz.admin.v1.auth.OAuth2AuthFeature;
import net.krotscheck.kangaroo.authz.admin.v1.resource.ApplicationService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.AuthenticatorService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.ClientService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.ConfigService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.OAuthTokenService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.RoleScopeService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.RoleService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.ScopeService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.UserIdentityService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.UserService;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorFeature;
import net.krotscheck.kangaroo.authz.common.cors.AuthzCORSFeature;
import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.httpClient.HttpClientFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.common.logging.LoggingFeature;
import net.krotscheck.kangaroo.common.security.SecurityFeature;
import net.krotscheck.kangaroo.common.status.StatusFeature;
import net.krotscheck.kangaroo.common.swagger.SwaggerFeature;
import net.krotscheck.kangaroo.common.timedtasks.TimedTasksFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;


/**
 * The OID Servlet application, including all configured resources and
 * features.
 *
 * @author Michael Krotscheck
 */
@SwaggerDefinition(
        info = @Info(
                title = "Authz Administration API",
                description = "",
                version = "v1"
        ),
        consumes = {"application/json"},
        produces = {"application/json"},

        schemes = {SwaggerDefinition.Scheme.HTTPS},
        securityDefinition = @SecurityDefinition(
                oAuth2Definitions = @OAuth2Definition(
                        key = "Kangaroo",
                        flow = Flow.PASSWORD,
                        authorizationUrl = "/oauth2/authorization",
                        tokenUrl = "/oauth2/token",
                        scopes = {
                                @io.swagger.annotations.Scope(
                                        name = Scope.APPLICATION,
                                        description = "Edit your applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.APPLICATION_ADMIN,
                                        description = "Edit all applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.IDENTITY,
                                        description = "Edit user identities"
                                                + " in your application."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.IDENTITY_ADMIN,
                                        description = "Edit user identities"
                                                + " in all applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.USER,
                                        description = "Edit users in your"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.USER_ADMIN,
                                        description = "Edit users in all"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.SCOPE,
                                        description = "Edit scopes in your"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.SCOPE_ADMIN,
                                        description = "Edit scopes in all"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.ROLE,
                                        description = "Edit roles in your"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.ROLE_ADMIN,
                                        description = "Edit roles in all"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.TOKEN,
                                        description = "Edit tokens in your"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.TOKEN_ADMIN,
                                        description = "Edit tokens in all"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.CLIENT,
                                        description = "Edit clients in your"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.CLIENT_ADMIN,
                                        description = "Edit clients in all"
                                                + " applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.AUTHENTICATOR,
                                        description = "Edit authenticators in"
                                                + " your applications."
                                ),
                                @io.swagger.annotations.Scope(
                                        name = Scope.AUTHENTICATOR_ADMIN,
                                        description = "Edit authenticators in"
                                                + " all applications."
                                )
                        }
                )
        )
)
public final class AdminV1API extends ResourceConfig {

    /**
     * Constructor. Creates a new application instance.
     */
    public AdminV1API() {
        // No autodiscovery, we load everything explicitly.
        property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);

        // Common features.
        register(ConfigurationFeature.class);    // Configuration loader
        register(JacksonFeature.class);          // Data Type de/serialization.
        register(ExceptionFeature.class);        // Exception Mapping.
        register(DatabaseFeature.class);         // Database Feature.
        register(StatusFeature.class);           // Heartbeat service.
        register(SecurityFeature.class);         // Security components.
        register(TimedTasksFeature.class);       // Timed tasks service.
        register(AuthenticatorFeature.class);    // OAuth2 Authenticators
        register(AuthzCORSFeature.class);        // CORS feature.
        register(HttpClientFeature.class);       // Make Http requests.
        register(LoggingFeature.class);          // API logging feature.

        // Swagger UI & API Documentation.
        register(new SwaggerFeature("net.krotscheck.kangaroo.authz.admin"));

        // Internal components
        register(new ServletConfigFactory.Binder());
        register(new FirstRunContainerLifecycleListener.Binder());

        // API Authentication and Authorization.
        register(OAuth2AuthFeature.class);

        // API Resources
        register(ConfigService.class);
        register(ApplicationService.class);
        register(AuthenticatorService.class);
        register(ScopeService.class);
        register(ClientService.class);
        register(RoleService.class);
        register(UserService.class);
        register(UserIdentityService.class);
        register(OAuthTokenService.class);

        // API Subresources
        register(new RoleScopeService.Binder());
    }
}
