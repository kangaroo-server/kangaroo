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

import javax.xml.ws.WebServiceException;

/**
 * Errors thrown during the OAuth authentication flow. These are remapped in the
 * response filter, so that they end up in the right place.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractOAuthException extends WebServiceException {

    /**
     * The error.
     */
    private String error;

    /**
     * The error description.
     */
    private String errorDescription;

    /**
     * Create a new OAuth error with a specific type and message.
     *
     * @param error            The error type.
     * @param errorDescription Human readable error description.
     */
    public AbstractOAuthException(final String error,
                                  final String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
    }

    /**
     * Get the error.
     *
     * @return The error code.
     */
    public final String getError() {
        return error;
    }

    /**
     * Get the error description.
     *
     * @return A human-readable error description.
     */
    public final String getErrorDescription() {
        return errorDescription;
    }
}
