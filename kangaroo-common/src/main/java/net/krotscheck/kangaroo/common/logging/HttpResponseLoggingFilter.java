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

package net.krotscheck.kangaroo.common.logging;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status.Family;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;

/**
 * This filter (running at priority 0) is a final-introspection filter that
 * provides insight into the HTTP traffic of a running server.
 */
@Priority(0)
public final class HttpResponseLoggingFilter
        implements ContainerResponseFilter {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(HttpResponseLoggingFilter.class);

    /**
     * Provided the request and the response, log out some basic information
     * during runtime.
     *
     * @param requestContext  The incoming request.
     * @param responseContext The outgoing response.
     */
    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) {
        // Grab the request method
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        int status = responseContext.getStatus();

        String message = extractResponseMessage(responseContext);
        String logMessage = String.format("%s HTTP %s %s%s",
                status, method, path, message).trim();

        logger.info(logMessage);
    }

    /**
     * Provided a response context, attempt to (safely) extract an error
     * message, if appropriate.
     *
     * @param context The response to extract the message from.
     * @return A string with the message, or emptystring.
     */
    private String extractResponseMessage(
            final ContainerResponseContext context) {

        // Enum switches can't be fully covered by jacoco.
        Family f = context.getStatusInfo().getFamily();

        if (f.equals(REDIRECTION)) {
            return " -> " + Optional.ofNullable(context.getHeaders())
                    .filter(h -> h.containsKey(HttpHeaders.LOCATION))
                    .map(h -> h.getFirst(HttpHeaders.LOCATION))
                    .map(Object::toString)
                    .orElse("No location header provided");
        }

        if (f.equals(CLIENT_ERROR) || f.equals(SERVER_ERROR)) {
            return ": " + Optional.ofNullable(context.getEntity())
                    .map(e -> (ErrorResponse) e)
                    .map(ErrorResponse::getErrorDescription)
                    .orElse("No error entity detected.");
        }

        return "";
    }
}
