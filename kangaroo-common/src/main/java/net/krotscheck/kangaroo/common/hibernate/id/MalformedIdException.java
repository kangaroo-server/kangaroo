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

package net.krotscheck.kangaroo.common.hibernate.id;

import javax.ws.rs.BadRequestException;

/**
 * Exception thrown if a field intended to be a Base-16 encoded BigInteger
 * is malformed or cannot be decoded.
 *
 * @author Michael Krotscheck
 */
public class MalformedIdException extends BadRequestException {

    /**
     * The exception message.
     */
    static final String MESSAGE = "Invalid resource identifier.";

    /**
     * Construct a new MalformedIdException with the provided message.
     */
    public MalformedIdException() {
        super(MESSAGE);
    }
}
