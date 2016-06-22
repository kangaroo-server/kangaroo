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
 * Commonly used requestMapping for when a particular property is missing or
 * incorrect. Can be used to easily inform the user that their input parameters
 * are janky.
 *
 * @author Michael Krotscheck
 */
public final class HttpInvalidFieldException extends HttpStatusException {

    /**
     * The invalid field name.
     */
    private String invalidField = "";

    /**
     * Constructs a new requestMapping with the specified detail message and
     * httpStatus.
     *
     * @param invalidFieldName The name of the invalid field.
     */
    public HttpInvalidFieldException(final String invalidFieldName) {
        this(invalidFieldName, null);
    }

    /**
     * Create a new redirecting HttpInvalidFieldException.
     *
     * @param invalidFieldName The name of the invalid field.
     * @param redirect         The URI to send the user agent to.
     */
    public HttpInvalidFieldException(final String invalidFieldName,
                                     final URI redirect) {
        super(HttpStatus.SC_BAD_REQUEST,
                "Invalid Field: " + invalidFieldName,
                redirect);
        this.invalidField = "foo";
    }

    /**
     * Returns the invalid field from this requestMapping.
     *
     * @return The name of the invalid field.
     */
    public String getInvalidField() {
        return invalidField;
    }
}
