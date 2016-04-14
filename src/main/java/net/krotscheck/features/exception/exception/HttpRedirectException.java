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

import org.apache.http.HttpStatus;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * An error thrown when we'd like to redirect the user to a different location.
 *
 * @author Michael Krotscheck
 */
public final class HttpRedirectException extends HttpStatusException {

    /**
     * The invalid field name.
     */
    private final URL redirectUrl;

    /**
     * Constructs a new requestMapping with the specified detail message and
     * httpStatus.
     *
     * @param redirectionUrl The URL to redirect the user to.
     */
    public HttpRedirectException(final String redirectionUrl) {
        super(HttpStatus.SC_SEE_OTHER);

        URL redirect;
        try {
            redirect = new URL(redirectionUrl);
        } catch (MalformedURLException mue) {
            redirect = null;
        }

        redirectUrl = redirect;
    }

    /**
     * Constructs a new requestMapping with the specified detail message and
     * httpStatus.
     *
     * @param redirectionUrl The URL to redirect the user to.
     */
    public HttpRedirectException(final URL redirectionUrl) {
        super(HttpStatus.SC_SEE_OTHER);
        redirectUrl = redirectionUrl;
    }

    /**
     * Returns the invalid field from this requestMapping.
     *
     * @return The redirection URL to which the user will be sent.
     */
    public URL getRedirectUrl() {
        return redirectUrl;
    }
}
