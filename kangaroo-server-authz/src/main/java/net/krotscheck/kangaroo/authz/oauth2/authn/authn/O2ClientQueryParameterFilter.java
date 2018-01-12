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

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidClientException;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.glassfish.jersey.server.ContainerRequest;
import org.hibernate.Session;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Optional;


/**
 * This filter's job is to set client context in the event that a client ID
 * is passed via a query parameter in a GET request. This usually occurs in
 * redirect-based flows, as triggered by the authorization endpoint
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHENTICATION)
public final class O2ClientQueryParameterFilter
        extends AbstractO2AuthenticationFilter {

    /**
     * Create a new instance of this filter.
     *
     * @param requestProvider The request provider.
     * @param sessionProvider The session provider.
     */
    public O2ClientQueryParameterFilter(
            final Provider<ContainerRequest> requestProvider,
            final Provider<Session> sessionProvider) {
        super(requestProvider, sessionProvider);
    }

    /**
     * Attempt to extract a client_id from the query string. This method ONLY
     * permits public clients, as client secrets may not be passed in URL's.
     *
     * @param context The request context.
     */
    @Override
    public void filter(final ContainerRequestContext context) {
        String method = context.getMethod();

        // Only GETs are permitted.
        if (!method.equals(HttpMethod.GET)) {
            return;
        }

        // Try to get the query parameters.
        MultivaluedMap<String, String> queryParameters =
                context.getUriInfo().getQueryParameters();

        // Throw if there's a client_secret, we can't let these things be
        // cached in the browser.
        if (queryParameters.containsKey("client_secret")) {
            throw new BadRequestException();
        }

        // If none found, pass on to next filter.
        if (!queryParameters.containsKey("client_id")) {
            return;
        }

        List<String> rawClientIds = queryParameters.get("client_id");
        if (rawClientIds.size() > 1) {
            throw new InvalidClientException();
        }

        try {
            Client client = Optional.of(rawClientIds)
                    .map(l -> l.get(0))
                    .map(IdUtil::fromString)
                    .map(id -> getSession().get(Client.class, id))
                    .filter(Client::isPublic)
                    .orElse(null);

            if (client == null) {
                throw new AccessDeniedException();
            }

            O2Principal principal = new O2Principal(client);
            setPrincipal(principal);
        } catch (NumberFormatException nfe) {
            // Not a parseable ID, so bad request.
            throw new BadRequestException();
        }
    }
}
