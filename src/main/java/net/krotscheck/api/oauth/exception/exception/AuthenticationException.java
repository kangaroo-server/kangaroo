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
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1.2.1">https://tools.ietf.org/html/rfc6749#section-4.1.2.1</a>
 * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
 */
public final class AuthenticationException {

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
            super("invalid_request", message);
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
            super("unauthorized_client", message);
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
            super("access_denied", message);
        }
    }

    /**
     * The authorization server does not support obtaining an authorization code
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
            super("unsupported_response_type", message);
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
            super("invalid_scope", message);
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
            super("server_error", message);
        }
    }

    /**
     * The authorization server is currently unable to handle the request due to
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
            super("temporarily_unavailable", message);
        }
    }


    /**
     * The Authorization Server requires End-User interaction of some form to
     * proceed. This error MAY be returned when the prompt parameter value in
     * the Authentication Request is none, but the Authentication Request cannot
     * be completed without displaying a user interface for End-User
     * interaction.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class InteractionRequiredException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InteractionRequiredException() {
            this("Direct user interaction required.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InteractionRequiredException(final String message) {
            super("interaction_required", message);
        }
    }

    /**
     * The Authorization Server requires End-User authentication. This error MAY
     * be returned when the prompt parameter value in the Authentication Request
     * is none, but the Authentication Request cannot be completed without
     * displaying a user interface for End-User authentication.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class LoginRequiredException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public LoginRequiredException() {
            this("End-User authentication required.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public LoginRequiredException(final String message) {
            super("login_required", message);
        }
    }

    /**
     * The End-User is REQUIRED to select a session at the Authorization Server.
     * The End-User MAY be authenticated at the Authorization Server with
     * different associated accounts, but the End-User did not select a session.
     * This error MAY be returned when the prompt parameter value in the
     * Authentication Request is none, but the Authentication Request cannot be
     * completed without displaying a user interface to prompt for a session to
     * use.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class AccountSelectionRequiredException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public AccountSelectionRequiredException() {
            this("Please authenticate with the authorization server.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public AccountSelectionRequiredException(final String message) {
            super("account_selection_required", message);
        }
    }

    /**
     * The Authorization Server requires End-User consent. This error MAY be
     * returned when the prompt parameter value in the Authentication Request is
     * none, but the Authentication Request cannot be completed without
     * displaying a user interface for End-User consent.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class ConsentRequiredException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public ConsentRequiredException() {
            this("End-user consent required.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public ConsentRequiredException(final String message) {
            super("consent_required", message);
        }
    }

    /**
     * The request_uri in the Authorization Request returns an error or contains
     * invalid data.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class InvalidRequestUriException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidRequestUriException() {
            this("Invalid request_uri.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidRequestUriException(final String message) {
            super("invalid_request_uri", message);
        }
    }

    /**
     * The request parameter contains an invalid Request Object.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class InvalidRequestObjectException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public InvalidRequestObjectException() {
            this("Invalid Request Object.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public InvalidRequestObjectException(final String message) {
            super("invalid_request_object", message);
        }
    }

    /**
     * The OP does not support use of the request parameter defined in Section
     * 6.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class RequestNotSupportedException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public RequestNotSupportedException() {
            this("The request parameter is not supported.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public RequestNotSupportedException(final String message) {
            super("request_not_supported", message);
        }
    }

    /**
     * The OP does not support use of the request_uri parameter defined in
     * Section 6.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class RequestUriNotSupportedException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public RequestUriNotSupportedException() {
            this("The request_uri parameter is not supported.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public RequestUriNotSupportedException(final String message) {
            super("request_uri_not_supported", message);
        }
    }

    /**
     * The OP does not support use of the registration parameter defined in
     * Section 7.2.1.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthError">http://openid.net/specs/openid-connect-core-1_0.html#AuthError</a>
     */
    public static final class RegistrationNotSupportedException
            extends AbstractOAuthException {

        /**
         * Create a new exception with the default message.
         */
        public RegistrationNotSupportedException() {
            this("The registration parameter is not supported.");
        }

        /**
         * Create a new exception with a custom message.
         *
         * @param message A user-friendly message.
         */
        public RegistrationNotSupportedException(final String message) {
            super("registration_not_supported", message);
        }
    }

    /**
     * Utility class, private constructor.
     */
    private AuthenticationException() {
    }
}
