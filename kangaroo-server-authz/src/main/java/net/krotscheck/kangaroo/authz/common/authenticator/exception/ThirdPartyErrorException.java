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
import net.krotscheck.kangaroo.util.StringUtil;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import java.util.Map;

/**
 * Error thrown if the third party application has a problem.
 *
 * @author Michael Krotscheck
 */
public class ThirdPartyErrorException extends KangarooException {

    /**
     * The error code for this exception.
     */
    private static final ErrorCode CODE = new ErrorCode(
            Status.SERVICE_UNAVAILABLE,
            "service_unavailable",
            "Unexpected error from an external service."
    );

    /**
     * Create a new exception with the specified error code.
     */
    public ThirdPartyErrorException() {
        super(CODE);
    }

    /**
     * Attempt to create an instance of this exception from a map of parameters.
     *
     * @param parameters The parameters to map.
     */
    public ThirdPartyErrorException(
            final Map<String, String> parameters) {
        this(parameters.get("error"), parameters.get("error_description"));
    }

    /**
     * Attempt to create an instance of this exception from a multivalued map
     * of parameters.
     *
     * @param parameters The parameters to map.
     */
    public ThirdPartyErrorException(
            final MultivaluedMap<String, String> parameters) {
        this(parameters.getFirst("error"),
                parameters.getFirst("error_description"));
    }

    /**
     * Create an exception with a specific response code and description.
     *
     * @param code        The code.
     * @param description The description.
     */
    public ThirdPartyErrorException(final String code,
                                    final String description) {
        super(new ErrorCode(Status.SERVICE_UNAVAILABLE,
                StringUtil.sameOrDefault(code,
                        CODE.getError()),
                StringUtil.sameOrDefault(description,
                        CODE.getErrorDescription())));
    }
}
