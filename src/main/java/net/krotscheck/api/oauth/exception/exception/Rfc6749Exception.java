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

import net.krotscheck.features.exception.exception.HttpStatusException;
import org.apache.http.HttpStatus;

import java.net.URI;

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
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidRequestException() {
            this("This request is invalid.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public InvalidRequestException(final URI redirect) {
            this("This request is invalid.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message  A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public InvalidRequestException(final String message,
                                       final URI redirect) {
            super(HttpStatus.SC_BAD_REQUEST,
                    message,
                    ErrorCode.INVALID_REQUEST,
                    redirect);
        }
    }

    /**
     * The client is not authorized to request an authorization code using this
     * method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class UnauthorizedClientException
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public UnauthorizedClientException() {
            this("This client is not authorized.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public UnauthorizedClientException(final URI redirect) {
            this("This client is not authorized.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message  A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public UnauthorizedClientException(final String message,
                                           final URI redirect) {
            super(HttpStatus.SC_UNAUTHORIZED,
                    message,
                    ErrorCode.UNAUTHORIZED_CLIENT,
                    redirect);
        }
    }

    /**
     * The resource owner or authorization server denied the request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class AccessDeniedException
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public AccessDeniedException() {
            this("Access denied.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public AccessDeniedException(final URI redirect) {
            this("Access denied.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message  A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public AccessDeniedException(final String message,
                                     final URI redirect) {
            super(HttpStatus.SC_UNAUTHORIZED,
                    message,
                    ErrorCode.ACCESS_DENIED,
                    redirect);
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
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public UnsupportedResponseType() {
            this("The requested response type is not supported.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public UnsupportedResponseType(final URI redirect) {
            this("The requested response type is not supported.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public UnsupportedResponseType(final String message,
                                       final URI redirect) {
            super(HttpStatus.SC_BAD_REQUEST,
                    message,
                    ErrorCode.UNSUPPORTED_RESPONSE_TYPE,
                    redirect);
        }
    }

    /**
     * The requested scope is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class InvalidScopeException
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidScopeException() {
            this("The requested scope is not valid.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public InvalidScopeException(final URI redirect) {
            this("The requested scope is not valid.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public InvalidScopeException(final String message,
                                     final URI redirect) {
            super(HttpStatus.SC_BAD_REQUEST,
                    message,
                    ErrorCode.INVALID_SCOPE,
                    redirect);
        }
    }

    /**
     * The requested client is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidClientException
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidClientException() {
            this("The requested client is not valid.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public InvalidClientException(final URI redirect) {
            this("The requested client is not valid.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public InvalidClientException(final String message,
                                      final URI redirect) {
            super(HttpStatus.SC_BAD_REQUEST,
                    message,
                    ErrorCode.INVALID_CLIENT,
                    redirect);
        }
    }

    /**
     * The requested grant type is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidGrantException
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidGrantException() {
            this("The requested grant is not valid.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public InvalidGrantException(final URI redirect) {
            this("The requested grant is not valid.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public InvalidGrantException(final String message,
                                     final URI redirect) {
            super(HttpStatus.SC_BAD_REQUEST,
                    message,
                    ErrorCode.INVALID_GRANT,
                    redirect);
        }
    }

    /**
     * The requested grant type is not supported.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class UnsupportedGrantTypeException
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public UnsupportedGrantTypeException() {
            this("The requested grant type is not supported.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public UnsupportedGrantTypeException(final URI redirect) {
            this("The requested grant type is not supported.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public UnsupportedGrantTypeException(final String message,
                                             final URI redirect) {
            super(HttpStatus.SC_BAD_REQUEST,
                    message,
                    ErrorCode.UNSUPPORTED_GRANT_TYPE,
                    redirect);
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
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public ServerErrorException() {
            this("Internal Server Error.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public ServerErrorException(final URI redirect) {
            this("Internal Server Error.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public ServerErrorException(final String message,
                                    final URI redirect) {
            super(HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    message,
                    ErrorCode.SERVER_ERROR,
                    redirect);
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
            extends HttpStatusException {

        /**
         * Create a new exception with the default message.
         */
        public TemporarilyUnavailableException() {
            this("The service is temporarily unavailable.", null);
        }

        /**
         * Create a redirecting exception with the default message.
         *
         * @param redirect The URI to redirect the user to.
         */
        public TemporarilyUnavailableException(final URI redirect) {
            this("The service is temporarily unavailable.", redirect);
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         * @param redirect The URI to redirect the user to.
         */
        public TemporarilyUnavailableException(final String message,
                                               final URI redirect) {
            super(HttpStatus.SC_BAD_REQUEST,
                    message,
                    ErrorCode.TEMPORARILY_UNAVAILABLE,
                    redirect);
        }
    }

    /**
     * Utility class, private constructor.
     */
    private Rfc6749Exception() {
    }
}
