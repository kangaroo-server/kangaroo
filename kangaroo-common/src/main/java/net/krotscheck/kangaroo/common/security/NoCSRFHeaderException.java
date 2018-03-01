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

package net.krotscheck.kangaroo.common.security;

import javax.ws.rs.BadRequestException;

/**
 * Exception thrown if our Cross-Site-Request-Forgery protections are not in
 * place.
 *
 * @author Michael Krotscheck
 */
final class NoCSRFHeaderException extends BadRequestException {

    /**
     * The exception message.
     */
    static final String MESSAGE =
            "No CSRF Header found, expected any value in X-Requested-With";

    /**
     * Construct a new BadRequestException with the provided message.
     */
    NoCSRFHeaderException() {
        super(MESSAGE);
    }
}
