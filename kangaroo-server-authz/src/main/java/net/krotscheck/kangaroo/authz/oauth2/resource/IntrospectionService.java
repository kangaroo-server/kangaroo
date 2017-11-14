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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Introspection endpoint of the OAuth2 service, complies with RFC-7662.
 *
 * @author Michael Krotscheck
 */
@Path("/introspect")
@PermitAll
@Transactional
@Api(tags = "OAuth2")
public final class IntrospectionService {

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
    public IntrospectionService(final Session session,
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
    @Produces(MediaType.APPLICATION_JSON)
    @O2Client(permitPublic = false)
    @O2BearerToken()
    @ApiOperation(value = "OAuth2 Introspection endpoint.")
    public Response introspectionRequest(
            @FormParam("token") final BigInteger tokenId) {

        // Pull the principal, to check the style of introspection request
        // we've got here.
        O2Principal principal = ObjectUtil
                .safeCast(securityContext.getUserPrincipal(), O2Principal.class)
                .orElseThrow(AccessDeniedException::new);
        O2AuthScheme scheme = O2AuthScheme.valueOf(principal.getScheme());

        OAuthToken introspectedToken;

        if (scheme.equals(O2AuthScheme.BearerToken)) {
            // If authorized via a bearer token, make sure the token matches the
            // requested introspection ID.
            introspectedToken = Optional
                    .ofNullable(principal.getOAuthToken())
                    .filter(t -> t.getId().equals(tokenId))
                    .orElse(null);
        } else {
            // If authorized via a bearer token, make sure the token matches the
            // requested introspection ID.
            Client client = principal.getContext();
            introspectedToken = Optional
                    .ofNullable(tokenId)
                    .map(id -> session.get(OAuthToken.class, id))
                    .filter(token -> token.getClient()
                            .getApplication()
                            .equals(client.getApplication()))
                    .orElse(null);
        }

        IntrospectionResponseEntity responseEntity =
                Optional.ofNullable(introspectedToken)
                        .map(IntrospectionResponseEntity::new)
                        .orElse(new IntrospectionResponseEntity());

        return Response.ok(responseEntity).build();
    }
}
