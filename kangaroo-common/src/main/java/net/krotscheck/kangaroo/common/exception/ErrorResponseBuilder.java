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
 */

package net.krotscheck.kangaroo.common.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import net.krotscheck.kangaroo.common.exception.KangarooException.ErrorCode;
import org.apache.http.impl.EnglishReasonPhraseCatalog;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Locale;
import java.util.Set;

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
    public static ErrorResponseBuilder from(final Status httpStatus) {
        return from(httpStatus,
                messageForCode(httpStatus),
                errorForCode(httpStatus));
    }

    /**
     * Create an error response object from a status code and a message.
     *
     * @param httpStatus The HTTP Status code to return.
     * @param message    The error message to provide.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final Status httpStatus,
                                            final String message) {
        return from(httpStatus,
                message,
                errorForCode(httpStatus));
    }

    /**
     * Create an error response object from a status code, a message, and an
     * error string.
     *
     * @param httpStatus The HTTP Status code to return.
     * @param message    The error message to provide.
     * @param error      A short error code.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final Status httpStatus,
                                            final String message,
                                            final String error) {
        ErrorResponseBuilder builder = new ErrorResponseBuilder();
        builder.response.httpStatus = httpStatus;
        builder.response.error = error;
        builder.response.errorDescription = message;

        return builder;
    }

    /**
     * Exception mapper for JSON parse exceptions - throw the json error back
     * at the user.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final JsonParseException e) {
        return from(Status.BAD_REQUEST,
                e.getMessage(),
                errorForCode(Status.BAD_REQUEST));
    }

    /**
     * Return an error object constructed from a jersey exception.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final WebApplicationException e) {
        return from(Status.fromStatusCode(e.getResponse().getStatus()),
                e.getMessage());
    }

    /**
     * Return an error object constructed from a kangaroo error code.
     *
     * @param ke A Kangaroo Exception.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final KangarooException ke) {
        return from(ke.getCode());
    }

    /**
     * Return an error object constructed from a kangaroo error code.
     *
     * @param code The error code.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final ErrorCode code) {
        return from(code.getHttpStatus(),
                code.getErrorDescription(),
                code.getError());
    }

    /**
     * Return an error object constructed from a generic unknown
     * requestMapping.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(
            final ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        if (violations.size() > 0) {
            return from(Status.BAD_REQUEST,
                    violations.iterator().next().getMessage());
        }
        return from(Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Return an error object constructed from a generic unknown
     * requestMapping.
     *
     * @param e The exception to map.
     * @return This builder.
     */
    public static ErrorResponseBuilder from(final Throwable e) {
        return from(Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper method that converts a status code to an HTTP status
     * requestMapping.
     *
     * @param httpStatus The HTTP status.
     * @return The HTTP Phrase catalog for this status.
     */
    private static String messageForCode(final Status httpStatus) {
        return EnglishReasonPhraseCatalog.INSTANCE
                .getReason(httpStatus.getStatusCode(), Locale.getDefault());
    }

    /**
     * Helper method that converts a status code to a short-form error code.
     *
     * @param httpStatus The HTTP status.
     * @return The HTTP Phrase catalog for this status.
     */
    private static String errorForCode(final Status httpStatus) {
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
        return Response.status(response.httpStatus)
                .type(MediaType.APPLICATION_JSON)
                .entity(response)
                .build();
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
        @JsonProperty("error_description")
        private String errorDescription = "";

        /**
         * The error code.
         */
        @JsonIgnore
        private Status httpStatus = Status.BAD_REQUEST;

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
         * Get the error description.
         *
         * @return The error description.
         */
        public String getErrorDescription() {
            return errorDescription;
        }

        /**
         * Get the HTTP status code.
         *
         * @return The http status code of the response.
         */
        public Status getHttpStatus() {
            return httpStatus;
        }
    }
}
