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

package net.krotscheck.kangaroo.util;

import javax.ws.rs.BadRequestException;

/**
 * The Request Utility was unable to reduce the incoming HTTP request into a
 * valid protocol, host, and port. This usually indicates a severe
 * misconfiguration problem.
 *
 * @author Michael Krotscheck
 */
class InvalidHostException extends BadRequestException {

    /**
     * The exception message.
     */
    static final String MESSAGE =
            "Unable to extract request protocol, host, or port.";

    /**
     * Construct a new BadRequestException with the provided message.
     *
     * @param e The cause of this exception.
     */
    InvalidHostException(final Throwable e) {
        super(MESSAGE, e);
    }
}
