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

package net.krotscheck.kangaroo.authz.oauth2.authn.authn;

import com.google.common.base.Strings;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2SecurityContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.glassfish.jersey.server.ContainerRequest;
import org.hibernate.Session;

import javax.inject.Provider;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * An abstract O2 authentication filter. This class's job is to provide common
 * injection points, and to check the current security context for a
 * successful auth. If a successful auth has been detected, any subsequent
 * filters should throw.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractO2AuthenticationFilter
        implements ContainerRequestFilter {

    /**
     * The request provider.
     */
    private final Provider<ContainerRequest> requestProvider;

    /**
     * The request's session provider.
     */
    private final Provider<Session> sessionProvider;

    /**
     * Create a new instance of this filter.
     *
     * @param requestProvider The request provider.
     * @param sessionProvider The session provider.
     */
    public AbstractO2AuthenticationFilter(
            final Provider<ContainerRequest> requestProvider,
            final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
        this.requestProvider = requestProvider;
    }

    /**
     * Get a session from the underlying session provider.
     *
     * @return A session, or null.
     */
    final Session getSession() {
        return this.sessionProvider.get();
    }

    /**
     * Get the container request, from the underlying request.
     *
     * @return A session, or null.
     */
    final ContainerRequest getRequest() {
        return this.requestProvider.get();
    }

    /**
     * Get the security context from the request context.
     *
     * @return The current active security context.
     */
    final SecurityContext getSecurityContext() {
        return getRequest().getSecurityContext();
    }

    /**
     * Set a principal to the security context.
     *
     * @param newPrincipal The new principal.
     */
    protected final void setPrincipal(final O2Principal newPrincipal) {
        SecurityContext context = getSecurityContext();
        O2Principal principal =
                Optional.ofNullable(context)
                        .map(c -> newPrincipal.merge(c.getUserPrincipal()))
                        .orElse(newPrincipal);

        O2SecurityContext newContext = new O2SecurityContext(principal);
        getRequest().setSecurityContext(newContext);
    }

    /**
     * Convert a raw ID and secret, into a BigInteger entry and value.
     *
     * @param rawId     The raw ID.
     * @param rawSecret The raw secret.
     * @return An entryset if at least the raw ID is valid. Otherwise null.
     */
    protected final Entry<BigInteger, String> convertCredentials(
            final String rawId,
            final String rawSecret) {
        try {
            String secret = Strings.emptyToNull(rawSecret);

            return Optional
                    .ofNullable(rawId)
                    .map(IdUtil::fromString)
                    .map(id -> new SimpleEntry<>(id, secret))
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
