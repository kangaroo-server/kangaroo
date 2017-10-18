/*
 * Copyright (c) 2017 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.common.authenticator.exception;

import net.krotscheck.kangaroo.common.exception.KangarooException;

import javax.ws.rs.core.Response.Status;

/**
 * Error thrown when an authenticator is not properly configured. It makes
 * use of the 501 HTTP status code, indicating that use of this authenticator
 * is not properly "implemented" (e.g. configured).
 *
 * @author Michael Krotscheck
 */
public class MisconfiguredAuthenticatorException extends KangarooException {

    /**
     * The error code for this exception.
     */
    public static final ErrorCode CODE = new ErrorCode(
            Status.BAD_REQUEST,
            "misconfigured",
            "This service is not properly configured."
    );

    /**
     * Create a new exception with the specified error code.
     */
    public MisconfiguredAuthenticatorException() {
        super(CODE);
    }
}
