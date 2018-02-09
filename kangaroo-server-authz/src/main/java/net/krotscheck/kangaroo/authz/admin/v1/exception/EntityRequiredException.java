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
 * This exception is thrown when an external reference - such as a linked
 * record of some kind, is invalid. THis could occur either if the reference
 * is malformed, does not exist, or we attempted to link to a resource that
 * we do not have the access rights for.
 *
 * @author Michael Krotscheck
 */
public final class EntityRequiredException extends BadRequestException {

    /**
     * The exception message.
     */
    static final String MESSAGE =
            "An entity is required to complete this operation.";

    /**
     * Construct a new BadRequestException with the provided message.
     */
    public EntityRequiredException() {
        super(MESSAGE);
    }

}
