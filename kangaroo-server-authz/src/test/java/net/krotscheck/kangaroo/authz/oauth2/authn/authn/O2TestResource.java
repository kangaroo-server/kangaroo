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
     * A GET method that is client authorized.
     *
     * @param c The injected security context.
     * @return The response.
     */
    @O2Client
    @GET
    public Response authorizedGET(@Context final SecurityContext c) {
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
    public Response authorizedPOST(@Context final SecurityContext c) {
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
    public Response authorizedPUT(@Context final SecurityContext c) {
        return Response.ok().build();
    }
}
