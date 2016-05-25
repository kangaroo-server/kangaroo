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

package net.krotscheck.api.oauth.resource;

import net.krotscheck.api.oauth.annotation.OAuthFilterChain;

import javax.annotation.security.PermitAll;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The token service for our OAuth2 provider.
 *
 * @author Michael Krotscheck
 */
@Path("/token")
@PermitAll
@OAuthFilterChain
public final class TokenService {

    /**
     * A stubbed resource.
     *
     * @return 200 OK
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenRequest() {
        return Response.ok().build();
    }
}
