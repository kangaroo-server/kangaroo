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

package net.krotscheck.kangaroo.authz.oauth2.exception;

import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

/**
 * This is a wrapper exception for unexpected exceptions, which need to be
 * redirected as per the OAuth2 Specification. It requires a throwable, as
 * this exception is only an expression of an OAuth2 targeted exception
 * response.
 *
 * @author Michael Krotscheck
 */
public final class RedirectingException extends WebApplicationException {

    /**
     * A redirect, required.
     */
    private final URI redirect;

    /**
     * The client type. This is needed to decide whether the response should
     * be placed in the fragment, or in the query string.
     */
    private final ClientType clientType;

    /**
     * Create a new RedirectingException for a provided exception, including
     * the redirect to which the details should be sent.
     *
     * @param cause      The cause.
     * @param redirect   The redirect.
     * @param clientType The client type.
     */
    public RedirectingException(final Throwable cause,
                                final URI redirect,
                                final ClientType clientType) {
        super(cause.getMessage(), cause, Status.FOUND);
        this.redirect = redirect;
        this.clientType = clientType;
    }

    /**
     * Return the provided redirect.
     *
     * @return The redirect. Could be null.
     */
    public URI getRedirect() {
        return redirect;
    }

    /**
     * Return the client type that threw this exception.
     *
     * @return The client type.
     */
    public ClientType getClientType() {
        return clientType;
    }

}
