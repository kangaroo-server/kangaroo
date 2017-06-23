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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.annotation.OAuthFilterChain;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidGrantException;
import net.krotscheck.kangaroo.authz.oauth2.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.authz.oauth2.resource.grant.IGrantTypeHandler;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import org.glassfish.hk2.api.ServiceLocator;
import org.hibernate.Session;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


/**
 * The token service for our OAuth2 provider.
 *
 * @author Michael Krotscheck
 */
@Path("/token")
@PermitAll
@OAuthFilterChain
@Transactional
public final class TokenService {

    /**
     * Hibernate session to use.
     */
    private final Session session;

    /**
     * Client credentials.
     */
    private final Credentials credentials;

    /**
     * Service locator.
     */
    private final ServiceLocator locator;

    /**
     * Create a new token service.
     *
     * @param session     Injected hibernate session.
     * @param credentials Injected, resolved client credentials.
     * @param locator     Service locator, to find the appropriate request
     *                    handler.
     */
    @Inject
    public TokenService(final Session session,
                        final Credentials credentials,
                        final ServiceLocator locator) {
        this.session = session;
        this.credentials = credentials;
        this.locator = locator;
    }

    /**
     * Attempt to issue a token based on the requested grant type.
     *
     * @param uriInfo   The raw HTTP request.
     * @param formData  The form post data.
     * @param grantType The requested granttype.
     * @return The processed response.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenRequest(
            @Context final UriInfo uriInfo,
            final MultivaluedMap<String, String> formData,
            @FormParam("grant_type") @DefaultValue("") final String grantType) {

        // Resolve the client - validation is handled by the filters.
        Client client = session.get(Client.class, credentials.getLogin());

        // Get and validate the grant type.
        IGrantTypeHandler handler = locator.getService(
                IGrantTypeHandler.class, grantType);
        if (handler == null) {
            throw new InvalidGrantException();
        }

        // Build the token.
        TokenResponseEntity e = handler.handle(client, formData);

        // Return the token.
        return Response.ok().entity(e).build();
    }
}
