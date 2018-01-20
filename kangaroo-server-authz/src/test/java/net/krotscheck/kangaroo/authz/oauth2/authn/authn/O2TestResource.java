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

import net.krotscheck.kangaroo.authz.oauth2.authn.O2BearerToken;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Client;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Testing resource for this suite of tests.
 *
 * @author Michael Krotscheck
 */
@Path("/")
public final class O2TestResource {

    /**
     * A resource is both client and token authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2BearerToken
    @O2Client
    @GET
    @Path("/multi")
    public Response multiAuthorizedGet(@Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A GET method that is client authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2Client
    @GET
    @Path("/client")
    public Response clientAuthorizedGET(@Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A POST method that is client authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2Client
    @POST
    @Path("/client")
    public Response clientAuthorizedPOST(@Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A PUT method that is client authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2Client
    @PUT
    @Path("/client")
    public Response clientAuthorizedPUT(@Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A POST method that is private-only authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2Client(permitPublic = false)
    @POST
    @Path("/client/private")
    public Response authorizedPrivatePOST(@Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A POST method that is private-only authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2Client(permitPrivate = false)
    @POST
    @Path("/client/public")
    public Response authorizedPublicPOST(@Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A GET method that is bearer token authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2BearerToken
    @GET
    @Path("/token")
    public Response bearerAuthorizedGet(@Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A GET method that is bearer token authorized, permitting only private
     * clients.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2BearerToken(permitPrivate = false)
    @GET
    @Path("/token/public")
    public Response bearerAuthorizedGetPrivate(
            @Context final SecurityContext c) {
        return Response.ok().build();
    }

    /**
     * A GET method that is bearer token authorized, permitting only public
     * clients.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2BearerToken(permitPublic = false)
    @GET
    @Path("/token/private")
    public Response bearerAuthorizedGetPublic(
            @Context final SecurityContext c) {
        return Response.ok().build();
    }
}
