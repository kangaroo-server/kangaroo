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

package net.krotscheck.kangaroo.common.exception;

import javax.ws.rs.core.Response.Status;

/**
 * Throw this exception if you detect that the system is in some form of a
 * blocking maintenance cycle. The paired exception mapper should also add
 * the retry header.
 *
 * @author Michael Krotscheck
 */
public final class SystemMaintenanceException extends KangarooException {

    /**
     * Error code for this exception.
     */
    public static final ErrorCode CODE = new ErrorCode(
            Status.SERVICE_UNAVAILABLE,
            "maintenance",
            "The system is in maintenance, please try again later.");

    /**
     * Create a new exception with the specified error code.
     */
    public SystemMaintenanceException() {
        super(CODE);
    }
}
