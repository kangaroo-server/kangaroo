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

package net.krotscheck.api.oauth.exception.exception;

/**
 * Exceptions defined in the OIC User Info Error specification.
 *
 * @author Michael Krotscheck
 * @see <a href="https://tools.ietf.org/html/rfc6750#section-3.1">https://tools.ietf.org/html/rfc6750#section-3.1</a>
 * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#UserInfoError">http://openid.net/specs/openid-connect-core-1_0.html#UserInfoError</a>
 */
public final class UserInfoException {

    /**
     * The request is missing a required parameter, includes an unsupported
     * parameter or parameter value, repeats the same parameter, uses more than
     * one method for including an access token, or is otherwise malformed.  The
     * resource server SHOULD respond with the HTTP 400 (Bad Request) status
     * code.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6750#section-3.1">https://tools.ietf.org/html/rfc6750#section-3.1</a>
     */
    public static final class InvalidRequestException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidRequestException() {
            this("This request is invalid.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidRequestException(final String message) {
            super("invalid_request", message);
        }
    }

    /**
     * The access token provided is expired, revoked, malformed, or invalid for
     * other reasons.  The resource SHOULD respond with the HTTP 401
     * (Unauthorized) status code.  The client MAY request a new access token
     * and retry the protected resource request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6750#section-3.1">https://tools.ietf.org/html/rfc6750#section-3.1</a>
     */
    public static final class InvalidTokenException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidTokenException() {
            this("This token is invalid.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidTokenException(final String message) {
            super("invalid_token", message);
        }
    }

    /**
     * The request requires higher privileges than provided by the access token.
     * The resource server SHOULD respond with the HTTP 403 (Forbidden) status
     * code and MAY include the "scope" attribute with the scope necessary to
     * access the protected resource.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6750#section-3.1">https://tools.ietf.org/html/rfc6750#section-3.1</a>
     */
    public static final class InsufficientScopeException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InsufficientScopeException() {
            this("The request requires higher privileges than provided by"
                    + " the access token.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InsufficientScopeException(final String message) {
            super("insufficient_scope", message);
        }
    }


    /**
     * Utility class, private constructor.
     */
    private UserInfoException() {
    }
}
