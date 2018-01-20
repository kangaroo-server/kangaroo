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

package net.krotscheck.kangaroo.authz.oauth2.authn;

import net.krotscheck.kangaroo.authz.oauth2.authn.authn.O2BearerTokenFilter;
import net.krotscheck.kangaroo.authz.oauth2.authn.authn.O2ClientBasicAuthFilter;
import net.krotscheck.kangaroo.authz.oauth2.authn.authn.O2ClientBodyFilter;
import net.krotscheck.kangaroo.authz.oauth2.authn.authn.O2ClientQueryParameterFilter;
import net.krotscheck.kangaroo.authz.oauth2.authn.authz.O2AuthorizationFilter;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature includes all functionality required to establish a valid
 * authentication context for each request on the OAuth2 server. It includes
 * validating clients, client authentication, browser identification, and
 * maintaining browser session state.
 *
 * @author Michael Krotscheck
 */
public final class O2AuthDynamicFeature implements DynamicFeature {

    /**
     * Session provider.
     */
    private final Provider<Session> sessionProvider;

    /**
     * The provider for the current request context's request.
     */
    private final Provider<ContainerRequest> requestProvider;

    /**
     * Feature initialization - including some provider injections.
     *
     * @param sessionProvider The Hibernate Session provider, needed for the
     *                        feature.
     * @param requestProvider The provider for the current request contexts'
     *                        http request.
     */
    @Inject
    O2AuthDynamicFeature(final Provider<Session> sessionProvider,
                         final Provider<ContainerRequest>
                                 requestProvider) {
        this.sessionProvider = sessionProvider;
        this.requestProvider = requestProvider;
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

        Boolean client = am.isAnnotationPresent(O2Client.class);
        Boolean token = am.isAnnotationPresent(O2BearerToken.class);

        if (client) {
            O2Client clientAT = am.getAnnotation(O2Client.class);

            Boolean permitPrivate = clientAT.permitPrivate();
            Boolean permitPublic = clientAT.permitPublic();

            if (permitPublic) {
                configuration.register(new O2ClientQueryParameterFilter(
                        requestProvider, sessionProvider));
            }
            if (permitPrivate) {
                configuration.register(new O2ClientBasicAuthFilter(
                        requestProvider, sessionProvider));
            }

            configuration.register(new O2ClientBodyFilter(
                    requestProvider, sessionProvider,
                    permitPrivate, permitPublic));
        }

        if (token) {
            O2BearerToken tokenAT = am.getAnnotation(O2BearerToken.class);
            configuration.register(new O2BearerTokenFilter(
                    requestProvider,
                    sessionProvider,
                    tokenAT.permitPrivate(),
                    tokenAT.permitPublic()
            ));
        }

        if (client || token) {
            configuration.register(new O2AuthorizationFilter());
        }
    }
}
