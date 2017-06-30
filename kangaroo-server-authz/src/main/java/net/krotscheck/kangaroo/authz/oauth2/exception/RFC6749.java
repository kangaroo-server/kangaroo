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

package net.krotscheck.kangaroo.authz.oauth2.exception;

import net.krotscheck.kangaroo.common.exception.KangarooException;

import javax.ws.rs.core.Response.Status;

/**
 * Error codes specified in the OAuth2 Specification.
 *
 * @author Michael Krotscheck
 * @see <a href="https://tools.ietf.org/html/rfc6749">https://tools.ietf.org/html/rfc6749</a>
 */
public final class RFC6749 {

    /**
     * Utility class, private constructor.
     */
    private RFC6749() {

    }

    /**
     * The request is missing a required parameter, includes an invalid
     * parameter value, includes a parameter more than once, or is otherwise
     * malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class InvalidRequestException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
                "invalid_request",
                "This request is invalid.");

        /**
         * Constructor.
         */
        public InvalidRequestException() {
            super(CODE);
        }
    }


    /**
     * The client is not authorized to request an authorization code using this
     * method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class UnauthorizedClientException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.UNAUTHORIZED,
                "unauthorized_client",
                "This client is not authorized.");

        /**
         * Constructor.
         */
        public UnauthorizedClientException() {
            super(CODE);
        }
    }

    /**
     * The resource owner or authorization server denied the request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class AccessDeniedException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.UNAUTHORIZED,
                "access_denied",
                "Access denied.");

        /**
         * Constructor.
         */
        public AccessDeniedException() {
            super(CODE);
        }
    }

    /**
     * The authorization server does not support obtaining an authorization
     * code using this method.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class UnsupportedResponseTypeException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
                "unsupported_response_type",
                "The requested response type is not supported.");

        /**
         * Constructor.
         */
        public UnsupportedResponseTypeException() {
            super(CODE);
        }
    }

    /**
     * The requested scope is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
     */
    public static final class InvalidScopeException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
                "invalid_scope",
                "The requested scope is not valid.");

        /**
         * Constructor.
         */
        public InvalidScopeException() {
            super(CODE);
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
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.INTERNAL_SERVER_ERROR,
                "server_error",
                "Internal Server Error.");

        /**
         * Constructor.
         */
        public ServerErrorException() {
            super(CODE);
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
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
                "temporarily_unavailable",
                "The service is temporarily unavailable.");

        /**
         * Constructor.
         */
        public TemporarilyUnavailableException() {
            super(CODE);
        }
    }

    /**
     * The requested client is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidClientException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
                "invalid_client",
                "The requested client is not valid.");

        /**
         * Constructor.
         */
        public InvalidClientException() {
            super(CODE);
        }
    }

    /**
     * The requested grant type is invalid, unknown, or malformed.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class InvalidGrantException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
                "invalid_grant",
                "The requested grant is not valid.");

        /**
         * Constructor.
         */
        public InvalidGrantException() {
            super(CODE);
        }
    }

    /**
     * The requested grant type is not supported.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.2">https://tools.ietf.org/html/rfc6749#section-5.2</a>
     */
    public static final class UnsupportedGrantTypeException
            extends KangarooException {

        /**
         * Error code for this exception.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
                "unsupported_grant_type",
                "The requested grant type is not supported.");

        /**
         * Constructor.
         */
        public UnsupportedGrantTypeException() {
            super(CODE);
        }
    }
}
