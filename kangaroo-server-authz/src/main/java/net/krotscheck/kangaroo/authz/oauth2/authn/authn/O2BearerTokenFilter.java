/*
 * Copyright (c) 2018 Michael Krotscheck
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

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.glassfish.jersey.server.ContainerRequest;
import org.hibernate.Session;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType.Bearer;
import static org.apache.http.HttpHeaders.AUTHORIZATION;


/**
 * This filter's job is to permit authentication on OAuth endpoints via an
 * already issued bearer token. In this case, no special scope is required,
 * however care should be taken at the resource endpoint to appropriately
 * scope this style of request.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHENTICATION)
public final class O2BearerTokenFilter
        extends AbstractO2AuthenticationFilter {

    /**
     * HTTP Bearer header matching.
     */
    private static final Pattern BEARER =
            Pattern.compile("^Bearer ([a-f0-9]{32})$", CASE_INSENSITIVE);

    /**
     * Permit private clients.
     */
    private final Boolean permitPrivate;

    /**
     * Permit public clients.
     */
    private final Boolean permitPublic;

    /**
     * Create a new instance of this filter.
     *
     * @param requestProvider The request provider.
     * @param sessionProvider The session provider.
     * @param permitPrivate   Whether private clients are permitted.
     * @param permitPublic    Whether public clients are permitted.
     */
    public O2BearerTokenFilter(final Provider<ContainerRequest> requestProvider,
                               final Provider<Session> sessionProvider,
                               final boolean permitPrivate,
                               final boolean permitPublic) {
        super(requestProvider, sessionProvider);
        this.permitPrivate = permitPrivate;
        this.permitPublic = permitPublic;
    }

    /**
     * Extract the authorization header. If it turns out to be a Bearer auth
     * client header, resolve that.
     *
     * @param request The request.
     */
    @Override
    public void filter(final ContainerRequestContext request) {

        // Is there an authorization header that matches the 'BEARER' pattern?
        Matcher authHeader = Optional

                // Pull the header.
                .ofNullable(request.getHeaderString(AUTHORIZATION))
                .map(String::trim)

                // Try to match against it.
                .map(BEARER::matcher)
                .filter(Matcher::matches)

                .orElse(null);

        // No fitting auth header has been found. Pass it on to another handler.
        if (authHeader == null) {
            return;
        }

        // Pull and decode the id.
        BigInteger bigId = Optional
                .ofNullable(authHeader.group(1))
                .map(IdUtil::fromString)
                .orElseThrow(BadRequestException::new);

        // If we have an ID, convert it into a non-expired bearer token.
        OAuthToken token = Optional.of(bigId)
                .map(id -> getSession().find(OAuthToken.class, id))
                .filter(t -> t.getTokenType().equals(Bearer))
                .filter(t -> !t.isExpired())
                .orElseThrow(AccessDeniedException::new);

        Client c = token.getClient();

        if (!c.isPublic().equals(permitPublic)
                && !c.isPrivate().equals(permitPrivate)) {
            throw new AccessDeniedException();
        }

        setPrincipal(new O2Principal(token));
    }
}
