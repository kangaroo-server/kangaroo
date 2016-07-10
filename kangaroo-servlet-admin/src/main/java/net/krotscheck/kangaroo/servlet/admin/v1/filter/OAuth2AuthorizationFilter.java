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

package net.krotscheck.kangaroo.servlet.admin.v1.filter;

import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.Session;

import java.io.IOException;
import java.security.Principal;
import java.util.SortedMap;
import java.util.UUID;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

/**
 * A request filter that validates a bearer token before permitting the
 * request to continue. It will replace the user principal with one that is
 * aware of the token's granted scopes.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHORIZATION)
@OAuth2
public final class OAuth2AuthorizationFilter implements ContainerRequestFilter {

    /**
     * The request's session provider.
     */
    private final Provider<Session> sessionProvider;

    /**
     * Create a new instance of this authorization filter.
     *
     * @param sessionProvider The context-relevant session provider.
     */
    @Inject
    public OAuth2AuthorizationFilter(final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    /**
     * This filter attempts to resolve an OAuth authorization token from the
     * request, and validate it. If successful, it will replace the
     * security context, otherwise it will leave it blank.
     *
     * @param requestContext request context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {
        String header =
                requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        OAuthToken token = getTokenFromHeader(header);

        // Blank token, and/or expired tokens, throw an exception.
        if (token == null || token.isExpired()) {
            return;
        }

        Boolean isSecure = requestContext.getSecurityContext().isSecure();
        SecurityContext context = new OAuthTokenContext(token, isSecure);
        requestContext.setSecurityContext(context);
    }

    /**
     * Extracts an OAuthToken from the authorization header string.
     *
     * @param header The header string.
     * @return An OAuth token, or null.
     */
    private OAuthToken getTokenFromHeader(final String header) {

        if (StringUtils.isEmpty(header)) {
            return null;
        }

        String[] token = header.split(" ");
        if (token.length != 2) {
            return null;
        }

        if (!token[0].equals("Bearer")) {
            return null;
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(token[1]);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }

        Session session = sessionProvider.get();
        OAuthToken oauthToken = session.get(OAuthToken.class, tokenId);

        if (oauthToken == null) {
            return null;
        }

        if (!oauthToken.getTokenType().equals(OAuthTokenType.Bearer)) {
            return null;
        }

        return oauthToken;
    }

    /**
     * Private security context implementation that validates against our
     * database of tokens.
     */
    public static final class OAuthTokenContext implements SecurityContext {

        /**
         * Is this secure?
         */
        private final boolean secure;

        /**
         * The principal.
         */
        private final UserIdentity principal;

        /**
         * The principal.
         */
        private final SortedMap<String, ApplicationScope> scopes;

        /**
         * Construct an authentication context from an OAuth token and a secure
         * flag.
         *
         * @param token    The OAuth token for this principal.
         * @param isSecure Whether to secure the context.
         */
        public OAuthTokenContext(final OAuthToken token,
                                 final Boolean isSecure) {
            // Materialize the scopes and the user identity.
            principal = token.getIdentity();
            scopes = token.getScopes();
            secure = isSecure;
        }

        /**
         * Return the current user identity.
         */
        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        /**
         * WARNING: OVERLOADED TERMS
         *
         * In order to simplify the declaration of scope permissions, this
         * method will check to see if the current user has been granted the
         * provied "scope" rather than "role".
         */
        @Override
        public boolean isUserInRole(final String roleName) {
            return scopes.containsKey(roleName);
        }

        /**
         * Was this request done via a secure request method?
         */
        @Override
        public boolean isSecure() {
            return secure;
        }

        /**
         * Get the authentication scheme.
         */
        @Override
        public String getAuthenticationScheme() {
            return "OAuth2";
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(OAuth2AuthorizationFilter.class)
                    .to(ContainerRequestFilter.class)
                    .in(Singleton.class);
        }
    }
}
