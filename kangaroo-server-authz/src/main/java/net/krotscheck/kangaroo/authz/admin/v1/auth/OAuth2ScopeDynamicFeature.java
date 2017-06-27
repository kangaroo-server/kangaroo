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

package net.krotscheck.kangaroo.authz.admin.v1.auth;

import net.krotscheck.kangaroo.authz.admin.v1.auth.filter.OAuth2AuthenticationFilter;
import net.krotscheck.kangaroo.authz.admin.v1.auth.filter.OAuth2AuthorizationFilter;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import org.apache.commons.configuration.Configuration;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.hibernate.Session;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature checks itself dynamically against all resources, so that
 * we can determine (at runtime) which token scopes are required for a
 * particular resource or resource method.
 *
 * It was heavily inspired by the ScopesAllowedDynamicFeature, with the
 * noted exception that both ForbiddenException and NotAuthorizedException
 * thrown will provide an accurate WWW-Authenticate header, based
 * on the scopes annotated to the resource.
 *
 * @author Michael Krotscheck
 */
final class OAuth2ScopeDynamicFeature implements DynamicFeature {

    /**
     * Session provider.
     */
    private final Provider<Session> sessionProvider;

    /**
     * The request's servlet configuration provider.
     */
    private final Provider<Configuration> configProvider;

    /**
     * Feature initialization - including some provider injections.
     *
     * @param sessionProvider The Hibernate Session provider, needed for the
     *                        feature.
     * @param configProvider  The system configuration provider.
     */
    @Inject
    OAuth2ScopeDynamicFeature(final Provider<Session> sessionProvider,
                                     @Named(ServletConfigFactory.GROUP_NAME)
                                     final Provider<Configuration>
                                             configProvider) {
        this.sessionProvider = sessionProvider;
        this.configProvider = configProvider;
    }

    /**
     * Given a resource, provide filters which check the identity and
     * scope grants needed by the resource itself.
     *
     * @param resourceInfo  The resource information.
     * @param configuration The feature configuration provider.
     */
    @Override
    public void configure(final ResourceInfo resourceInfo,
                          final FeatureContext configuration) {
        final AnnotatedMethod am =
                new AnnotatedMethod(resourceInfo.getResourceMethod());

        // DenyAll takes precedence over ScopesAllowed and PermitAll
        if (am.isAnnotationPresent(DenyAll.class)) {
            configuration.register(new OAuth2AuthenticationFilter(
                    sessionProvider,
                    configProvider,
                    new String[]{}));
            configuration.register(new OAuth2AuthorizationFilter());
            return;
        }

        // ScopesAllowed on the method takes precedence over PermitAll
        ScopesAllowed ra = am.getAnnotation(ScopesAllowed.class);
        if (ra != null) {
            configuration.register(new OAuth2AuthenticationFilter(
                    sessionProvider,
                    configProvider,
                    ra.value()));
            configuration.register(new OAuth2AuthorizationFilter(ra.value()));
            return;
        }

        // PermitAll takes precedence over ScopesAllowed on the class
        if (am.isAnnotationPresent(PermitAll.class)) {
            // Do nothing.
            return;
        }

        // DenyAll can't be attached to classes

        // ScopesAllowed on the class takes precedence over PermitAll
        ra = resourceInfo.getResourceClass().getAnnotation(ScopesAllowed.class);
        if (ra != null) {
            configuration.register(new OAuth2AuthenticationFilter(
                    sessionProvider,
                    configProvider,
                    ra.value()));
            configuration.register(new OAuth2AuthorizationFilter(ra.value()));
        }
    }
}
