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

package net.krotscheck.api.oauth.token.exception;

import net.krotscheck.api.oauth.exception.exception.AbstractOAuthException;

/**
 * Exceptions defined in the OIC Token Error specification.
 *
 * @author Michael Krotscheck
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
 * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#HybridTokenErrorResponse">http://openid.net/specs/openid-connect-core-1_0.html#HybridTokenErrorResponse</a>
 * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#RefreshErrorResponse">http://openid.net/specs/openid-connect-core-1_0.html#RefreshErrorResponse</a>
 */
public final class TokenException extends Exception {

    /**
     * The request is missing a required parameter, includes an unsupported
     * parameter value (other than grant type), repeats a parameter, includes
     * multiple credentials, utilizes more than one mechanism for authenticating
     * the client, or is otherwise malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
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
     * The client is not authorized to request an access token using this
     * method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class UnauthorizedClientException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public UnauthorizedClientException() {
            this("The client is not authorized to request an access token"
                    + " using this method.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public UnauthorizedClientException(final String message) {
            super("unauthorized_client", message);
        }
    }


    /**
     * The resource owner or authorization server denied the request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
     */
    public static final class AccessDeniedException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public AccessDeniedException() {
            this("The resource owner or authorization server denied the"
                    + " request.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public AccessDeniedException(final String message) {
            super("access_denied", message);
        }
    }

    /**
     * The authorization server does not support obtaining an access token using
     * this method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
     */
    public static final class UnsupportedResponseTypeException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public UnsupportedResponseTypeException() {
            this("The authorization server does not support obtaining an"
                    + " access token using this method.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public UnsupportedResponseTypeException(final String message) {
            super("unsupported_response_type", message);
        }
    }

    /**
     * The requested scope is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidScopeException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidScopeException() {
            this("The requested scope is invalid, unknown, or malformed.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidScopeException(final String message) {
            super("invalid_scope", message);
        }
    }

    /**
     * The authorization server encountered an unexpected condition that
     * prevented it from fulfilling the request. (This error code is needed
     * because a 500 Internal Server Error HTTP status code cannot be returned
     * to the client via an HTTP redirect.)
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
     */
    public static final class ServerErrorException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public ServerErrorException() {
            this("Internal Server Error.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public ServerErrorException(final String message) {
            super("server_error", message);
        }
    }

    /**
     * The authorization server is currently unable to handle the request due to
     * a temporary overloading or maintenance of the server.  (This error code
     * is needed because a 503 Service Unavailable HTTP status code cannot be
     * returned to the client via an HTTP redirect.)
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2.1">https://tools.ietf.org/html/rfc6749#section-4.2.2.1</a>
     */
    public static final class TemporarilyUnavailableException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public TemporarilyUnavailableException() {
            this("The authorization server is currently unable"
                    + " to handle the request.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public TemporarilyUnavailableException(final String message) {
            super("temporarily_unavailable", message);
        }
    }

    /**
     * Client authentication failed (e.g., unknown client, no client
     * authentication included, or unsupported authentication method).  The
     * authorization server MAY return an HTTP 401 (Unauthorized) status code to
     * indicate which HTTP authentication schemes are supported.  If the client
     * attempted to authenticate via the "Authorization" request header field,
     * the authorization server MUST respond with an HTTP 401 (Unauthorized)
     * status code and include the "WWW-Authenticate" response header field
     * matching the authentication scheme used by the client.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidClientException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidClientException() {
            this("The client is invalid.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidClientException(final String message) {
            super("invalid_client", message);
        }
    }

    /**
     * The provided authorization grant (e.g., authorization code, resource
     * owner credentials) or refresh token is invalid, expired, revoked, does
     * not match the redirection URI used in the authorization request, or was
     * issued to another client.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidGrantException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidGrantException() {
            this("The provided authorization grant is invalid.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidGrantException(final String message) {
            super("invalid_grant", message);
        }
    }

    /**
     * The authorization grant type is not supported by the authorization
     * server.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class UnsupportedGrantTypeException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public UnsupportedGrantTypeException() {
            this("The authorization grant type is not supported.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public UnsupportedGrantTypeException(final String message) {
            super("unsupported_grant_type", message);
        }
    }

    /**
     * Utility class, private constructor.
     */
    private TokenException() {
    }
}
