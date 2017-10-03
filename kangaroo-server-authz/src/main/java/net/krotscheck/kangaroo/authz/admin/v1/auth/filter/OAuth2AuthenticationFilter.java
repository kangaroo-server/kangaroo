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

package net.krotscheck.kangaroo.authz.admin.v1.auth.filter;

import net.krotscheck.kangaroo.authz.admin.v1.auth.OAuth2SecurityContext;
import net.krotscheck.kangaroo.authz.admin.v1.auth.exception.OAuth2NotAuthorizedException;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.math.BigInteger;

/**
 * A request filter that validates a bearer token before permitting the
 * request to continue. It will replace the user principal with one that is
 * aware of the token's granted scopes.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHENTICATION)
public final class OAuth2AuthenticationFilter
        implements ContainerRequestFilter {

    /**
     * The request's session provider.
     */
    private final Provider<Session> sessionProvider;

    /**
     * The request's servlet configuration provider.
     */
    private final Provider<Configuration> configProvider;

    /**
     * Short list of roles allowed for this particular resource.
     */
    private final String[] scopesAllowed;

    /**
     * Create a new filter, only permitting the provided scopes.
     *
     * @param configProvider  System configuration provider.
     * @param sessionProvider Hibernate Session provider.
     * @param scopesAllowed   The permitted scopes.
     */
    public OAuth2AuthenticationFilter(final Provider<Session> sessionProvider,
                                      final Provider<Configuration>
                                              configProvider,
                                      final String[] scopesAllowed) {
        this.scopesAllowed = scopesAllowed;
        this.sessionProvider = sessionProvider;
        this.configProvider = configProvider;
    }

    /**
     * This filter attempts to resolve an OAuth authorization token from the
     * request, and validate it. If successful, it will replace the
     * security context, otherwise it will leave it blank.
     *
     * @param request The container request context.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void filter(final ContainerRequestContext request)
            throws IOException {

        // First, if we don't have a security context, throw.
        SecurityContext oldContext = request.getSecurityContext();
        if (oldContext == null) {
            throw new OAuth2NotAuthorizedException(
                    request.getUriInfo(),
                    scopesAllowed);
        }

        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        BigInteger tokenId = getTokenIdFromHeader(header);
        Application a = loadAdminApplication();

        Session session = sessionProvider.get();
        session.getTransaction().begin();

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
        if (token == null || token.isExpired()) {
            session.getTransaction().commit();
            throw new OAuth2NotAuthorizedException(
                    request.getUriInfo(),
                    scopesAllowed);
        }

        SecurityContext context = new OAuth2SecurityContext(token, true);
        request.setSecurityContext(context);
        session.getTransaction().commit();
    }

    /**
     * Extracts an OAuthToken from the authorization header string.
     *
     * @param header The header string.
     * @return An OAuth token, or null.
     */
    private BigInteger getTokenIdFromHeader(final String header) {

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
            return IdUtil.fromString(token[1]);
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
        String idString = servletConfig.getString(Config.APPLICATION_ID);
        BigInteger appId = IdUtil.fromString(idString);

        Session s = sessionProvider.get();
        return s.get(Application.class, appId);
    }
}
