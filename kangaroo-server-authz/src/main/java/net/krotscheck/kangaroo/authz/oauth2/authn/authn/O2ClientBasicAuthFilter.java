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
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import org.glassfish.jersey.server.ContainerRequest;
import org.hibernate.Session;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

/**
 * This filter handles Http BASIC authentication, via client/secret pairs via
 * the authorization header.
 *
 * @author Michael Krotscheck
 */
@Priority(Priorities.AUTHENTICATION)
public final class O2ClientBasicAuthFilter
        extends AbstractO2AuthenticationFilter {

    /**
     * HTTP Basic header matching.
     */
    private static final Pattern BASIC =
            Pattern.compile("^Basic ([0-9a-zA-Z]+={0,2})$", CASE_INSENSITIVE);

    /**
     * A base-64 decoder.
     */
    private static final Decoder DECODER = Base64.getDecoder();

    /**
     * Character set.
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Create a new instance of this filter.
     *
     * @param requestProvider The request provider.
     * @param sessionProvider The session provider.
     */
    public O2ClientBasicAuthFilter(final Provider<ContainerRequest>
                                           requestProvider,
                                   final Provider<Session>
                                           sessionProvider) {
        super(requestProvider, sessionProvider);
    }

    /**
     * Extract the authorization header. If it turns out to be a Basic auth
     * client header, resolve that.
     *
     * @param request The request.
     */
    @Override
    public void filter(final ContainerRequestContext request) {

        // Is there an authorization header that matches the 'BASIC' pattern?
        Matcher authHeader = Optional

                // Pull the header.
                .ofNullable(request.getHeaderString(AUTHORIZATION))
                .map(String::trim)

                // Try to match against it.
                .map(BASIC::matcher)
                .filter(Matcher::matches)

                .orElse(null);

        // No fitting auth header has been found. Pass it on to another handler.
        if (authHeader == null) {
            return;
        }

        // Convert matched credentials.
        Entry<BigInteger, String> creds = Optional

                // Pull and decode the group.
                .ofNullable(authHeader.group(1))
                .map(s -> new String(DECODER.decode(s), UTF8))
                .map(Strings::nullToEmpty)

                // Split and convert to a tuple.
                .filter(s -> s.contains(":"))
                .map(s -> s.split(":", 2))
                .map(s -> convertCredentials(s[0], s[1]))

                // Require a secret.
                .filter(t -> nonNull(t.getValue()))

                // If the credentials cannot be parsed or are missing,
                // it's a bad request
                .orElseThrow(BadRequestException::new);

        // Require a valid client.
        Client client = Optional
                .of(creds.getKey())
                .map(id -> getSession().find(Client.class, id))
                .filter(c -> creds.getValue().equals(c.getClientSecret()))
                .orElseThrow(AccessDeniedException::new);

        // Valid client and auth.
        setPrincipal(new O2Principal(client));
    }
}
