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

package net.krotscheck.features.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import net.krotscheck.features.exception.exception.HttpRedirectException;
import net.krotscheck.features.exception.exception.HttpStatusException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.impl.EnglishReasonPhraseCatalog;

import java.util.Locale;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * An error response object that can be returned from our services. Jackson
 * should figure out how to decode it.
 *
 * @author Michael Krotscheck
 */
public final class ErrorResponseBuilder {

    /**
     * The error response which this builder exists to manage.
     */
    private final ErrorResponse response;

    /**
     * Creates a new error response.
     */
    public ErrorResponseBuilder() {
        this.response = new ErrorResponse();
    }

    /**
     * Create an error response object from a status code.
     *
     * @param httpStatus The HTTP Status code to return.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final int httpStatus) {
        return from(httpStatus,
                messageForCode(httpStatus),
                errorForCode(httpStatus),
                null);
    }

    /**
     * Create an error response object from a status code and a message.
     *
     * @param httpStatus The HTTP Status code to return.
     * @param message    The error message to provide.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final int httpStatus,
                                            final String message) {
        return from(httpStatus,
                message,
                errorForCode(httpStatus),
                null);
    }

    /**
     * Create an error response object from a status code and a message.
     *
     * @param httpStatus The HTTP Status code to return.
     * @param message    The error message to provide.
     * @param error      A short error code.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final int httpStatus,
                                            final String message,
                                            final String error) {
        return from(httpStatus, message, error, null);
    }

    /**
     * Create an error response object from a status code, a message, and a
     * redirect URL.
     *
     * @param httpStatus  The HTTP Status code to return.
     * @param message     The error message to provide.
     * @param error       A short error code.
     * @param redirectUrl The URL to redirect the request to.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final int httpStatus,
                                            final String message,
                                            final String error,
                                            final String redirectUrl) {
        ErrorResponseBuilder builder = new ErrorResponseBuilder();
        builder.response.httpStatus = httpStatus;
        builder.response.error = error;
        builder.response.errorMessage = message;
        builder.response.redirectUrl = redirectUrl;

        return builder;
    }

    /**
     * Exception mapper for JSON parse exceptions - throw the json error back at
     * the user.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final JsonParseException e) {
        ErrorResponseBuilder builder = new ErrorResponseBuilder();
        builder.response.httpStatus = HttpStatus.SC_BAD_REQUEST;
        builder.response.error = errorForCode(builder.response.httpStatus);
        builder.response.errorMessage = e.getMessage();

        return builder;
    }

    /**
     * Return an error response constructed from a status requestMapping.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final HttpStatusException e) {
        String redirectUrl = null;
        if (e instanceof HttpRedirectException) {
            redirectUrl =
                    ((HttpRedirectException) e).getRedirectUrl().toString();
        }

        return from(e.getHttpStatus(),
                e.getMessage(),
                errorForCode(e.getHttpStatus()),
                redirectUrl);
    }

    /**
     * Return an error object constructed from a jersey exception.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final WebApplicationException e) {
        return from(e.getResponse().getStatus());
    }

    /**
     * Return an error object constructed from a generic unknown
     * requestMapping.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final Exception e) {
        return from(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper method that converts a status code to an HTTP status
     * requestMapping.
     *
     * @param httpStatus The HTTP status.
     * @return The HTTP Phrase catalog for this status.
     */
    private static String messageForCode(final int httpStatus) {
        return EnglishReasonPhraseCatalog.INSTANCE
                .getReason(httpStatus, Locale.getDefault());
    }

    /**
     * Helper method that converts a status code to a short-form error code.
     *
     * @param httpStatus The HTTP status.
     * @return The HTTP Phrase catalog for this status.
     */
    private static String errorForCode(final int httpStatus) {
        return messageForCode(httpStatus)
                .toLowerCase()
                .replace(" ", "_");
    }

    /**
     * Build the response from this builder.
     *
     * @return HTTP Response object for this error.
     */
    public Response build() {
        if (StringUtils.isEmpty(response.redirectUrl)) {
            return Response.status(response.httpStatus)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(response)
                    .build();
        } else {
            UriBuilder builder = UriBuilder.fromPath(response.redirectUrl);
            builder.queryParam("error", response.error);
            builder.queryParam("http_status", response.httpStatus);
            builder.queryParam("error_message", response.errorMessage);

            return Response.status(HttpStatus.SC_MOVED_TEMPORARILY)
                    .header(HttpHeaders.LOCATION, builder.build())
                    .build();
        }
    }

    /**
     * Internal class used to encapsulate parameters from our error response.
     */
    public static final class ErrorResponse {

        /**
         * The error message.
         */
        private String error = "";

        /**
         * The error message.
         */
        private String errorMessage = "";

        /**
         * If this error includes an HTTP redirect (as with OAuth errors),
         * include it here. The error code and message will be appended. This is
         * ignored by JSON.
         */
        @JsonIgnore
        private String redirectUrl = "";

        /**
         * The error code.
         */
        private int httpStatus = HttpStatus.SC_BAD_REQUEST;

        /**
         * Private constructor.
         */
        private ErrorResponse() {
        }

        /**
         * Return a machine-readable error short-code.
         *
         * @return A short error identifier.
         */
        public String getError() {
            return error;
        }

        /**
         * Get the error message.
         *
         * @return The error message.
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * Get the redirect url.
         *
         * @return A redirect URL, if configured, otherwise "".
         */
        public String getRedirectUrl() {
            return redirectUrl;
        }

        /**
         * Get the HTTP status code.
         *
         * @return The http status code of the response.
         */
        public int getHttpStatus() {
            return httpStatus;
        }
    }
}
