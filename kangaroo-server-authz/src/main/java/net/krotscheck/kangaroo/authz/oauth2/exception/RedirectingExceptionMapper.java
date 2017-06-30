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

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.spi.ExceptionMappers;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * This exception mapper handles RedirectingExceptions thrown by the system.
 * It first invokes the existing ErrorResponseBuilder and then remaps the
 * response to an OAuth2 redirected exception.
 *
 * @author Michael Krotscheck
 */
public final class RedirectingExceptionMapper
        implements ExceptionMapper<RedirectingException> {

    /**
     * The service locator. This class makes use of all registered
     * ExceptionMappers to perform a first-pass of the exception before
     * turning it into a redirect. Since that creates a circular dependency,
     * we read it - on request - from the injector instead.
     */
    private final ServiceLocator serviceLocator;

    /**
     * Create a new exception mapper for our redirecting exceptions.
     *
     * @param serviceLocator The service locator.
     */
    @Inject
    RedirectingExceptionMapper(final ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    /**
     * Convert to response.
     *
     * @param exception The exception to convert.
     * @return A Response instance for this error.
     */
    public Response toResponse(final RedirectingException exception) {
        Throwable cause = exception.getCause();

        // Retrieve the exception mapper for this cause.
        ExceptionMappers mappers =
                serviceLocator.getService(ExceptionMappers.class);
        ExceptionMapper mapper = mappers.findMapping(cause);
        Response r = mapper.toResponse(cause);
        ErrorResponse responseEntity = (ErrorResponse) r.getEntity();

        // Build the Redirect URI from the response.
        UriBuilder builder = UriBuilder.fromUri(exception.getRedirect());
        builder.queryParam("error", responseEntity.getError());
        builder.queryParam("error_description",
                responseEntity.getErrorDescription());

        if (exception.getClientType().equals(ClientType.Implicit)) {
            // Generate the query, and feed it right back into the builder.
            builder.fragment(builder.build().getQuery());
            builder.replaceQuery("");
        }

        return Response.fromResponse(r)
                .status(Status.FOUND)
                .header(HttpHeaders.LOCATION, builder.build())
                .build();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(RedirectingExceptionMapper.class)
                    .to(ExceptionMapper.class)
                    .in(Singleton.class);
        }
    }
}
