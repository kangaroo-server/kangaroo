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

import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.ServletConfigFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;

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
     * The request's servlet configuration provider.
     */
    private final Provider<Configuration> configProvider;

    /**
     * Create a new instance of this authorization filter.
     *
     * @param sessionProvider The context-relevant session provider.
     * @param configProvider  Servlet Configuration Provider
     */
    @Inject
    public OAuth2AuthorizationFilter(final Provider<Session> sessionProvider,
                                     @Named(ServletConfigFactory.GROUP_NAME)
                                     final Provider<Configuration>
                                             configProvider) {
        this.sessionProvider = sessionProvider;
        this.configProvider = configProvider;
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
        UUID tokenId = getTokenIdFromHeader(header);
        Application a = loadAdminApplication();

        Session session = sessionProvider.get();
        Transaction t = session.beginTransaction();

        Criteria c = session.createCriteria(OAuthToken.class)
                .createAlias("client", "c")
                .createAlias("c.application", "a")
                .add(Restrictions.eq("id", tokenId))
                .add(Restrictions.eq("a.id", a.getId()))
                .add(Restrictions.eq("tokenType", OAuthTokenType.Bearer))
                .setMaxResults(1)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        OAuthToken token = (OAuthToken) c.uniqueResult();

        // Blank token, and/or expired tokens, throw an exception.
        if (token != null && !token.isExpired()) {
            Boolean isSecure = requestContext.getSecurityContext().isSecure();
            SecurityContext context = new OAuthTokenContext(token, isSecure);
            requestContext.setSecurityContext(context);
        }
        t.commit();
    }

    /**
     * Extracts an OAuthToken from the authorization header string.
     *
     * @param header The header string.
     * @return An OAuth token, or null.
     */
    private UUID getTokenIdFromHeader(final String header) {

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

        try {
            return UUID.fromString(token[1]);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /**
     * Resolve the admin application.
     *
     * @return The admin application as configured in the database.
     */
    private Application loadAdminApplication() {
        Configuration servletConfig = configProvider.get();
        String uuidString = servletConfig.getString(Config.APPLICATION_ID);
        UUID appId = UUID.fromString(uuidString);

        Session s = sessionProvider.get();
        return s.get(Application.class, appId);
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
        private final OAuthToken principal;

        /**
         * The scopes.
         */
        private final Set<String> scopes;

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
            principal = token;
            scopes = token.getScopes().keySet();
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
         * <p>
         * In order to simplify the declaration of scope permissions, this
         * method will check to see if the current user has been granted the
         * provied "scope" rather than "role".
         */
        @Override
        public boolean isUserInRole(final String roleName) {
            return scopes.contains(roleName);
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
