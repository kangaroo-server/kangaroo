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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2AuthScheme;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2BearerToken;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.util.ObjectUtil;
import org.hibernate.Session;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.Optional;

import static net.krotscheck.kangaroo.authz.common.database.entity.ClientType.ClientCredentials;

/**
 * Revocation endpoint of the OAuth2 service, complies with RFC-7009.
 *
 * @author Michael Krotscheck
 */
@Path("/oauth2/revoke")
@PermitAll
@Transactional
@Api(tags = "OAuth2")
public final class RevocationService {

    /**
     * Hibernate session to use.
     */
    private final Session session;

    /**
     * Security Context for this request..
     */
    private final SecurityContext securityContext;

    /**
     * Create a new introspection service.
     *
     * @param session         Injected hibernate session.
     * @param securityContext The security context for the current request.
     */
    @Inject
    public RevocationService(final Session session,
                             final SecurityContext securityContext) {
        this.session = session;
        this.securityContext = securityContext;
    }

    /**
     * Attempt to return the claims associated with this token.
     *
     * @param tokenId The token ID to introspect.
     * @return The processed response.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @O2Client(permitPublic = false)
    @O2BearerToken()
    @ApiOperation(value = "OAuth2 Revocation endpoint.")
    public Response introspectionRequest(
            @ApiParam(type = "string")
            @FormParam("token") final BigInteger tokenId) {

        // Pull the principal, to check the style of authorization we have here.
        O2Principal principal = ObjectUtil
                .safeCast(securityContext.getUserPrincipal(), O2Principal.class)
                .orElseThrow(AccessDeniedException::new);
        O2AuthScheme scheme = O2AuthScheme.valueOf(principal.getScheme());

        Optional<OAuthToken> revokedToken;

        OAuthToken authToken = principal.getOAuthToken();
        Client client = principal.getContext();

        if (client.getType().equals(ClientCredentials)
                || scheme.equals(O2AuthScheme.ClientPrivate)) {
            // We're authorized via the client, or via a token belonging to a
            // client-credentials client. In this case, any token may be
            // revoked, as long as it exists and belongs to the same
            // application.
            revokedToken = Optional.ofNullable(tokenId)
                    .map(id -> session.get(OAuthToken.class, id))
                    .filter(t -> t.getClient()
                            .getApplication()
                            .equals(client.getApplication()));
        } else {
            // We are authorizing via a non-client-credentials token, meaning
            // it's attached to a user and identity. In this case, any token
            // belonging to the user may be revoked.
            revokedToken = Optional.ofNullable(tokenId)
                    .map(id -> session.get(OAuthToken.class, id))
                    .filter(t -> t.getIdentity().getUser()
                            .equals(authToken.getIdentity().getUser()));
        }

        if (!revokedToken.isPresent()) {
            throw new NotFoundException();
        }

        session.delete(revokedToken.get());
        session.getTransaction().commit();

        return Response.status(Status.RESET_CONTENT).build();
    }
}
