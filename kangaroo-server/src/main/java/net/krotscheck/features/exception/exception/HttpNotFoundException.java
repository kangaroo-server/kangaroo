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

import java.net.URI;

/**
 * Convenience exception for 404-not-found.
 *
 * @author Michael Krotscheck
 */
public final class HttpNotFoundException extends HttpStatusException {

    /**
     * Create a new HttpNotFoundException.
     */
    public HttpNotFoundException() {
        super(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Create a new redirecting HttpNotFoundException.
     *
     * @param redirect The URI to send the user agent to.
     */
    public HttpNotFoundException(final URI redirect) {
        super(HttpStatus.SC_NOT_FOUND, redirect);
    }
}
