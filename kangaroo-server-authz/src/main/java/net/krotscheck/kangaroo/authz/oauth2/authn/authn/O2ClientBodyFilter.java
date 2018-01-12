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
import org.glassfish.jersey.server.ContainerRequest;
import org.hibernate.Session;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;


/**
 * This filter's job is to detect whether the request is attempting Client
 * Authorization, via a client_id and client_secret in the form body. If it does
 * not find both, it presumes that some other method of authentication was
 * used, and defers evaluation of the credentials to the (eventual)
 * authorization filter.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHENTICATION)
public final class O2ClientBodyFilter
        extends AbstractO2AuthenticationFilter {

    /**
     * Create a new instance of this filter.
     *
     * @param requestProvider The request provider.
     * @param sessionProvider The session provider.
     */
    public O2ClientBodyFilter(final Provider<ContainerRequest> requestProvider,
                              final Provider<Session> sessionProvider) {
        super(requestProvider, sessionProvider);
    }

    /**
     * Attempt to extract a client ID and/or secret from the form body.
     * If the form body is not available, then don't parse it.
     *
     * Note that if the resolved client contains a client_secret, then
     * passing this in either the form body or in the authorization header is
     * required.
     *
     * @param context The request context.
     */
    @Override
    public void filter(final ContainerRequestContext context) {
        String method = context.getMethod();

        // Only posts are permitted.
        if (!method.equals(HttpMethod.POST)) {
            return;
        }

        // Only form data is permitted.
        if (!context.getMediaType().equals(APPLICATION_FORM_URLENCODED_TYPE)) {
            return;
        }

        ContainerRequest request = getRequest();
        request.bufferEntity();

        Form body = request.readEntity(Form.class);
        MultivaluedMap<String, String> data = body.asMap();

        List<String> rawClientIds =
                data.getOrDefault("client_id", emptyList());
        List<String> rawClientSecrets =
                data.getOrDefault("client_secret", emptyList());

        // If either is larger than 1, throw.
        if (rawClientIds.size() > 1 || rawClientSecrets.size() > 1) {
            throw new BadRequestException();
        }
        if (rawClientIds.size() == 0) {
            return;
        }

        String rawClientId = Optional.of(rawClientIds)
                .map(l -> l.get(0))
                .orElse(null);
        String rawClientSecret = Optional.of(rawClientSecrets)
                .filter(l -> l.size() == 1)
                .map(l -> l.get(0))
                .orElse(null);

        Entry<BigInteger, String> creds =
                convertCredentials(rawClientId, rawClientSecret);

        // Cannot convert, so it's a bad request.
        if (creds == null) {
            throw new BadRequestException();
        }

        // Require a matching client.
        Client client = Optional
                .of(creds.getKey())
                .map(id -> getSession().find(Client.class, id))
                .filter(c -> Objects.equals(c.getClientSecret(),
                        creds.getValue()))
                .orElseThrow(AccessDeniedException::new);

        // Valid client and auth.
        setPrincipal(new O2Principal(client));
    }
}
