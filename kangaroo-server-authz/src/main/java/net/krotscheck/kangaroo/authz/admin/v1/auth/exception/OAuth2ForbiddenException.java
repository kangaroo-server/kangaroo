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

package net.krotscheck.kangaroo.authz.admin.v1.auth.exception;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * Throw this exception when you know who the user is, but they do not have
 * the appropriate token scopes to access this resource.
 *
 * @author Michael Krotscheck
 */
public class OAuth2ForbiddenException extends WWWChallengeException {

    /**
     * The error code for this exception.
     */
    public static final ErrorCode CODE = new ErrorCode(
            Status.FORBIDDEN,
            "forbidden",
            "This token may not access this resource."
    );

    /**
     * Create a new exception with the specified error code.
     *
     * @param requestInfo    The original URI request, from which we're
     *                       going to derive our realm.
     * @param requiredScopes A list of required scopes.
     */
    public OAuth2ForbiddenException(final UriInfo requestInfo,
                                    final String[] requiredScopes) {
        super(CODE, requestInfo, requiredScopes);
    }
}
