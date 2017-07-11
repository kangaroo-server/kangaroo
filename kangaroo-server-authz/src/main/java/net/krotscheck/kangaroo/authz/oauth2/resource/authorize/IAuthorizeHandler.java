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

package net.krotscheck.kangaroo.authz.oauth2.resource.authorize;

import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.AuthenticatorState;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.SortedMap;

/**
 * This interface describes a processing entity which handles a specific kind
 * of request against the /authorize endpoint.
 *
 * @author Michael Krotscheck
 */
public interface IAuthorizeHandler {

    /**
     * Handle a specific authorization grant request.
     *
     * @param uriInfo  The original request, in case additional data is needed.
     * @param auth     The authenticator to use to process this request.
     * @param redirect The redirect (already validated) to which the response
     *                 should be returned.
     * @param scopes   The (validated) list of scopes requested by the user.
     * @param state    The client's requested state ID.
     * @return A response entity with the appropriate response.
     */
    Response handle(UriInfo uriInfo,
                    Authenticator auth,
                    URI redirect,
                    SortedMap<String, ApplicationScope> scopes,
                    String state);

    /**
     * Handle a callback response from the IdP (Authenticator). Provided with
     * the previously stored state, this method should return to the client
     * either a valid token, or an appropriate error response.
     *
     * @param s       The request state previously saved by the client.
     * @param uriInfo The URI response from the third party IdP.
     * @return A response entity indicating success or failure.
     */
    Response callback(AuthenticatorState s, UriInfo uriInfo);

    /**
     * Provided a stored intermediate authenticator state, attempt to resolve
     * an instance of the associated authenticator implementation.
     *
     * @param state The state to resolve.
     * @return An authenticator Impl, available from the injection context.
     */
    IAuthenticator getAuthenticator(AuthenticatorState state);
}
