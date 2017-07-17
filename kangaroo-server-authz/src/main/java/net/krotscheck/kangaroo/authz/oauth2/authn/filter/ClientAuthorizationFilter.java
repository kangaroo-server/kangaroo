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

package net.krotscheck.kangaroo.authz.oauth2.authn.filter;

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.annotation.OAuthFilterChain;
import net.krotscheck.kangaroo.authz.oauth2.authn.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidClientException;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.Session;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

/**
 * This filter attempts to extract credentials from the HTTP request, and
 * authorizes the request based on those credentials.
 */
@Priority(Priorities.AUTHORIZATION)
@OAuthFilterChain
public final class ClientAuthorizationFilter implements ContainerRequestFilter {

    /**
     * The request's credential provider.
     */
    private final Provider<Credentials> credentialsProvider;

    /**
     * The request's session provider.
     */
    private final Provider<Session> sessionProvider;

    /**
     * Create a new authorization filter.
     *
     * @param credentialsProvider The credential provider.
     * @param sessionProvider     The session provider.
     */
    @Inject
    public ClientAuthorizationFilter(
            final Provider<Credentials> credentialsProvider,
            final Provider<Session> sessionProvider) {
        this.credentialsProvider = credentialsProvider;
        this.sessionProvider = sessionProvider;
    }

    /**
     * Extract the client ID from the various locations where it can live,
     * and attempt to resolve the client.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {
        Credentials c = credentialsProvider.get();
        Session s = sessionProvider.get();

        if (!c.isValid()) {
            throw new InvalidClientException();
        }

        // Try to read the client.
        Client client = s.get(Client.class, c.getLogin());
        if (client == null) {
            throw new InvalidClientException();
        }

        // Is the client private?
        Boolean isPrivate = !StringUtils.isEmpty(client.getClientSecret());
        if (isPrivate) {
            if (!client.getClientSecret().equals(c.getPassword())) {
                throw new AccessDeniedException();
            }
        } else {
            if (!StringUtils.isEmpty(c.getPassword())) {
                throw new AccessDeniedException();
            }
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(ClientAuthorizationFilter.class)
                    .to(ContainerRequestFilter.class)
                    .in(Singleton.class);
        }
    }
}
