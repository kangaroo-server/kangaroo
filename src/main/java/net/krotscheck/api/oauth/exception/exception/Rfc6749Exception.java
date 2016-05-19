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
 * Exceptions defined in the OIC Authentication Error specification.
 *
 * @author Michael Krotscheck
 * @see <a href="https://tools.ietf.org/html/rfc6749">https://tools.ietf.org/html/rfc6749</a>
 */
public final class Rfc6749Exception {

    /**
     * The request is missing a required parameter, includes an invalid
     * parameter value, includes a parameter more than once, or is otherwise
     * malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
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
            super(ErrorCode.INVALID_REQUEST, message);
        }
    }

    /**
     * The client is not authorized to request an authorization code using this
     * method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class UnauthorizedClientException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public UnauthorizedClientException() {
            this("This client is not authorized.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public UnauthorizedClientException(final String message) {
            super(ErrorCode.UNAUTHORIZED_CLIENT, message);
        }
    }

    /**
     * The resource owner or authorization server denied the request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class AccessDeniedException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public AccessDeniedException() {
            this("Access denied.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public AccessDeniedException(final String message) {
            super(ErrorCode.ACCESS_DENIED, message);
        }
    }

    /**
     * The authorization server does not support obtaining an authorization
     * code
     * using this method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class UnsupportedResponseType
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public UnsupportedResponseType() {
            this("The requested response type is not supported.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public UnsupportedResponseType(final String message) {
            super(ErrorCode.UNSUPPORTED_RESPONSE_TYPE, message);
        }
    }

    /**
     * The requested scope is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class InvalidScopeException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidScopeException() {
            this("The requested scope is not valid.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidScopeException(final String message) {
            super(ErrorCode.INVALID_SCOPE, message);
        }
    }

    /**
     * The requested client is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidClientException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidClientException() {
            this("The requested client is not valid.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidClientException(final String message) {
            super(ErrorCode.INVALID_CLIENT, message);
        }
    }

    /**
     * The requested grant type is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidGrantException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidGrantException() {
            this("The requested grant is not valid.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidGrantException(final String message) {
            super(ErrorCode.INVALID_GRANT, message);
        }
    }

    /**
     * The requested grant type is not supported.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class UnsupportedGrantTypeException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public UnsupportedGrantTypeException() {
            this("The requested grant type is not supported.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public UnsupportedGrantTypeException(final String message) {
            super(ErrorCode.UNSUPPORTED_GRANT_TYPE, message);
        }
    }

    /**
     * The authorization server encountered an unexpected condition that
     * prevented it from fulfilling the request. (This error code is needed
     * because a 500 Internal Server Error HTTP status code cannot be returned
     * to the client via an HTTP redirect.)
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
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
            super(ErrorCode.SERVER_ERROR, message);
        }
    }

    /**
     * The authorization server is currently unable to handle the request due
     * to
     * a temporary overloading or maintenance of the server.  (This error code
     * is needed because a 503 Service Unavailable HTTP status code cannot be
     * returned to the client via an HTTP redirect.)
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class TemporarilyUnavailableException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public TemporarilyUnavailableException() {
            this("The service is temporarily unavailable.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public TemporarilyUnavailableException(final String message) {
            super(ErrorCode.TEMPORARILY_UNAVAILABLE, message);
        }
    }

    /**
     * Utility class, private constructor.
     */
    private Rfc6749Exception() {
    }
}
