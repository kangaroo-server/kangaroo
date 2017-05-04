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

package net.krotscheck.kangaroo.common.exception.exception;

import javax.ws.rs.core.Response.Status;
import java.net.URI;

/**
 * Convenience exception for 403-forbidden.
 *
 * @author Michael Krotscheck
 */
public final class HttpForbiddenException extends HttpStatusException {

    /**
     * Create a new HttpForbiddenException.
     */
    public HttpForbiddenException() {
        super(Status.FORBIDDEN);
    }

    /**
     * Create a new redirecting HttpForbiddenException.
     *
     * @param redirect The URI to send the user agent to.
     */
    public HttpForbiddenException(final URI redirect) {
        super(Status.FORBIDDEN, redirect);
    }
}
