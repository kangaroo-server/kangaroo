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

package net.krotscheck.features.exception.exception;

import org.apache.http.impl.EnglishReasonPhraseCatalog;

import java.net.URI;
import java.util.Locale;
import javax.xml.ws.WebServiceException;

/**
 * A generic HTTP httpStatus requestMapping, which can be intercepted to
 * generate an HTTP server error response.
 *
 * @author Michael Krotscheck
 */
public class HttpStatusException extends WebServiceException {

    /**
     * The HTTP httpStatus code for this requestMapping.
     */
    private final int httpStatus;

    /**
     * An optional redirection URI.
     */
    private final URI redirect;

    /**
     * A short form error code.
     */
    private final String errorCode;

    /**
     * Constructs a new requestMapping with the specified httpStatus. The
     * message is inferred from the same.
     *
     * @param httpResponseStatus The HTTP Status code thrown by this error.
     */
    public HttpStatusException(final int httpResponseStatus) {
        this(httpResponseStatus, EnglishReasonPhraseCatalog.INSTANCE
                .getReason(httpResponseStatus, Locale.getDefault()));
    }

    /**
     * Constructs a new requestMapping with the specified httpStatus and
     * redirect.
     *
     * @param httpResponseStatus The HTTP Status code thrown by this error.
     * @param redirect           The redirection URI to which we're supposed
     *                           to send the user.
     */
    public HttpStatusException(final int httpResponseStatus,
                               final URI redirect) {
        this(httpResponseStatus,
                EnglishReasonPhraseCatalog.INSTANCE
                        .getReason(httpResponseStatus, Locale.getDefault()),
                redirect);
    }

    /**
     * Constructs a new requestMapping with the specified detail message and
     * httpStatus.
     *
     * @param httpResponseStatus The HTTP Status code thrown by this error.
     * @param message            The detail message which is later retrieved
     *                           using the getMessage method
     */
    public HttpStatusException(final int httpResponseStatus,
                               final String message) {
        this(httpResponseStatus,
                message,
                EnglishReasonPhraseCatalog.INSTANCE
                        .getReason(httpResponseStatus, Locale.getDefault())
                        .toLowerCase().replace(" ", "_"),
                null);
    }

    /**
     * Constructs a new requestMapping with the specified detail message,
     * httpStatus, and redirect.
     *
     * @param httpResponseStatus The HTTP Status code thrown by this error.
     * @param message            The detail message which is later retrieved
     *                           using the getMessage method
     * @param redirect           The redirection URI to which we're supposed
     *                           to send the user.
     */
    public HttpStatusException(final int httpResponseStatus,
                               final String message,
                               final URI redirect) {
        this(httpResponseStatus,
                message,
                EnglishReasonPhraseCatalog.INSTANCE
                        .getReason(httpResponseStatus, Locale.getDefault())
                        .toLowerCase().replace(" ", "_"),
                redirect);
    }

    /**
     * Constructs a new requestMapping with the specified detail message,
     * status, and error code.
     *
     * @param httpResponseStatus The HTTP Status code thrown by this error.
     * @param message            The detail message which is later retrieved
     *                           using the getMessage method
     * @param errorCode          A short-form error code.
     */
    public HttpStatusException(final int httpResponseStatus,
                               final String message,
                               final String errorCode) {
        this(httpResponseStatus, message, errorCode, null);
    }

    /**
     * Constructs a new requestMapping with the specified detail message,
     * status, error code, and redirection url. The mapper will redirect the
     * user to the provided URL with the error body in the query string
     *
     * @param httpResponseStatus The HTTP Status code thrown by this error.
     * @param message            The detail message which is later retrieved
     *                           using the getMessage method
     * @param errorCode          A short-form error code.
     * @param redirect           The redirection URI to which we're supposed
     *                           to send the user.
     */
    public HttpStatusException(final int httpResponseStatus,
                               final String message,
                               final String errorCode,
                               final URI redirect) {
        super(message);
        this.httpStatus = httpResponseStatus;
        this.errorCode = errorCode;
        this.redirect = redirect;
    }

    /**
     * Returns the httpStatus code.
     *
     * @return The HTTP Status for this response.
     */
    public final int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Retrieve the redirection URI.
     *
     * @return The redirect URI.
     */
    public final URI getRedirect() {
        return redirect;
    }

    /**
     * Return the short-form error code for this exception.
     *
     * @return A short-form error code.
     */
    public final String getErrorCode() {
        return errorCode;
    }
}
