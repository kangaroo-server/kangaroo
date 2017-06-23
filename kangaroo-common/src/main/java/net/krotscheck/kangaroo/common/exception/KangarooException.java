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

package net.krotscheck.kangaroo.common.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

/**
 * This error class should be the source for most of the expected errors in
 * our system. It accepts a configuration type from which it derives most of its
 * default values, as well as an optional 'redirect' value in case the error
 * needs to be sent to a referral_uri.
 *
 * This error type will generate its own response body, as there's two
 * slightly different behaviors, which are mandated across the entire
 * Kangaroo microservices architecture. As such, you should not need to write
 * an exception mapper for it.
 *
 * @author Michael Krotscheck
 */
public abstract class KangarooException extends WebApplicationException {

    /**
     * The error code used to construct this application.
     */
    private final ErrorCode code;

    /**
     * Create a new exception with the specified error code.
     *
     * @param code The error code enum type.
     */
    protected KangarooException(final ErrorCode code) {
        super(code.getError(),
                ErrorResponseBuilder.from(code).build());
        this.code = code;
    }

    /**
     * Create a new exception with an error code, and a redirect. This will
     * cause the response to return a 302, rather than whatever HTTP status
     * code is in the code itself. That value will instead be encoded into
     * the redirection URL.
     *
     * @param code     The error code.
     * @param redirect The redirect.
     */
    protected KangarooException(final ErrorCode code,
                                final URI redirect) {
        super(code.getError(),
                ErrorResponseBuilder.from(code, redirect).build());
        this.code = code;
    }

    /**
     * Get the error code.
     *
     * @return The error code.
     */
    public final ErrorCode getCode() {
        return code;
    }


    /**
     * This class encapsulates values necessary for initializing the
     * KangarooException.
     */
    public static final class ErrorCode {

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
        private final Status httpStatus;

        /**
         * Create a new enum instance.
         *
         * @param status           The HTTP status which we usually expect.
         * @param error            The error code, according to the RFC.
         * @param errorDescription The error description, human readable.
         */
        public ErrorCode(final Status status,
                         final String error,
                         final String errorDescription) {
            this.httpStatus = status;
            this.error = error;
            this.errorDescription = errorDescription;
        }

        /**
         * The HTTP Status usually assigned to this error type. This will be
         * overridden if the error response need to be sent via a redirect.
         *
         * @return The http status.
         */
        public Status getHttpStatus() {
            return httpStatus;
        }

        /**
         * Get the error code.
         *
         * @return The error code string. See RFC's for more data.
         */
        public String getError() {
            return error;
        }

        /**
         * Get the error description.
         *
         * @return The error description, human readable.
         */
        public String getErrorDescription() {
            return errorDescription;
        }
    }
}
