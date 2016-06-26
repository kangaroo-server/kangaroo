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

package net.krotscheck.kangaroo.common.exception.rfc6749;

/**
 * A list of error codes identified in RFC6749.
 *
 * @author Michael Krotscheck
 */
public final class ErrorCode {

    /**
     * Utility class - private constructor.
     */
    private ErrorCode() {
    }

    /**
     * The request is missing a required parameter, includes an
     * unsupported parameter value (other than grant type),
     * repeats a parameter, includes multiple credentials,
     * utilizes more than one mechanism for authenticating the
     * client, or is otherwise malformed.
     */
    public static final String INVALID_REQUEST =
            "invalid_request";

    /**
     * The client is not authorized to request a code or token
     * code using this method.
     */
    public static final String UNAUTHORIZED_CLIENT =
            "unauthorized_client";

    /**
     * The resource owner or authorization server denied the
     * request.
     */
    public static final String ACCESS_DENIED =
            "access_denied";

    /**
     * The authorization server does not support obtaining an
     * authorization code using this method.
     */
    public static final String UNSUPPORTED_RESPONSE_TYPE =
            "unsupported_response_type";

    /**
     * The requested scope is invalid, unknown, or malformed.
     */
    public static final String INVALID_SCOPE =
            "invalid_scope";

    /**
     * The authorization server encountered an unexpected
     * condition that prevented it from fulfilling the request.
     * (This error code is needed because a 500 Internal Server
     * Error HTTP status code cannot be returned to the client
     * via an HTTP redirect.)
     */
    public static final String SERVER_ERROR =
            "server_error";

    /**
     * The authorization server is currently unable to handle
     * the request due to a temporary overloading or maintenance
     * of the server.  (This error code is needed because a 503
     * Service Unavailable HTTP status code cannot be returned
     * to the client via an HTTP redirect.)
     */
    public static final String TEMPORARILY_UNAVAILABLE =
            "temporarily_unavailable";

    /**
     * Client authentication failed (e.g., unknown client, no
     * client authentication included, or unsupported
     * authentication method).  The authorization server MAY
     * return an HTTP 401 (Unauthorized) status code to indicate
     * which HTTP authentication schemes are supported.  If the
     * client attempted to authenticate via the "Authorization"
     * request header field, the authorization server MUST
     * respond with an HTTP 401 (Unauthorized) status code and
     * include the "WWW-Authenticate" response header field
     * matching the authentication scheme used by the client.
     */
    public static final String INVALID_CLIENT =
            "invalid_client";

    /**
     * The provided authorization grant (e.g., authorization
     * code, resource owner credentials) or refresh token is
     * invalid, expired, revoked, does not match the redirection
     * URI used in the authorization request, or was issued to
     * another client.
     */
    public static final String INVALID_GRANT =
            "invalid_grant";

    /**
     * The authorization grant type is not supported by the
     * authorization server.
     */
    public static final String UNSUPPORTED_GRANT_TYPE =
            "unsupported_grant_type";


}
