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

package net.krotscheck.api.oauth.authenticator;

import net.krotscheck.features.database.entity.Authenticator;
import net.krotscheck.features.database.entity.UserIdentity;

import java.net.URI;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * This interface describes the methods used during user authentication,
 * responsible for interfacing with a third party authentication provider.
 * All authentication MUST be performed via redirect.
 *
 * @author Michael Krotscheck
 */
public interface IAuthenticator {

    /**
     * Delegate an authentication request to a third party authentication
     * provider, such as Google, Facebook, etc.
     *
     * @param configuration The authenticator configuration.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return An HTTP response, redirecting the client to the next step.
     */
    Response delegate(Authenticator configuration,
                      URI callback);

    /**
     * Authenticate and/or create a user identity for a specific client, given
     * the URI from an authentication delegate.
     *
     * @param configuration The authenticator configuration.
     * @param parameters    Parameters for the authenticator, retrieved from
     *                      an appropriate source.
     * @return A user identity.
     */
    UserIdentity authenticate(Authenticator configuration,
                              MultivaluedMap<String, String> parameters);
}
