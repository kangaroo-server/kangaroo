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
    private int httpStatus;

    /**
     * Constructs a new requestMapping with the specified detail message and
     * httpStatus.
     *
     * @param httpResponseStatus The HTTP Status code thrown by this error.
     */
    public HttpStatusException(final int httpResponseStatus) {
        this(httpResponseStatus, EnglishReasonPhraseCatalog.INSTANCE
                .getReason(httpResponseStatus, Locale.getDefault()));
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
        super(message);
        this.httpStatus = httpResponseStatus;
    }

    /**
     * Returns the httpStatus code.
     *
     * @return The HTTP Status for this response.
     */
    public final int getHttpStatus() {
        return httpStatus;
    }


}
