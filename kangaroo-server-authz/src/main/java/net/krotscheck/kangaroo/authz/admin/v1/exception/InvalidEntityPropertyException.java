/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.admin.v1.exception;

import javax.ws.rs.BadRequestException;

/**
 * This exception is a catchall exception for requests that are malformed on a
 * per-property basis. While we can't give details on each property directly,
 * we can at least point the engineer at where to look.
 *
 * @author Michael Krotscheck
 */
public final class InvalidEntityPropertyException extends BadRequestException {

    /**
     * The exception message.
     */
    static final String MESSAGE = "The property '%s' is invalid.";

    /**
     * Construct a new exception with the provided property name, indicating
     * that the value is required or invalid.
     *
     * @param propertyName The name of the property.
     */
    public InvalidEntityPropertyException(final String propertyName) {
        super(String.format(MESSAGE, propertyName));
    }

}
