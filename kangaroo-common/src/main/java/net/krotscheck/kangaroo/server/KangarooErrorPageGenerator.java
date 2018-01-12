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

package net.krotscheck.kangaroo.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

/**
 * A factory class that allows us to build Kangaroo-style grizzly servers.
 *
 * @author Michael Krotscheck
 */
public final class KangarooErrorPageGenerator
        implements ErrorPageGenerator {

    /**
     * The object mapper used to convert our entities.
     */
    private static final ObjectMapper MAPPER =
            new ObjectMapperFactory().get();

    /**
     * Get the object mapper. This method is here only so we can mock it for
     * tests.
     *
     * @return The object mapper.
     */
    public ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Provided a request, annotate the response object with an appropriate
     * error.
     *
     * @param request      The request.
     * @param status       The HTTP status.
     * @param reasonPhrase The reason an error was thrown.
     * @param description  The description.
     * @param exception    Any throwables that caused this.
     * @return The response body.
     */
    @Override
    public String generate(final Request request,
                           final int status,
                           final String reasonPhrase,
                           final String description,
                           final Throwable exception) {
        Response r = request.getResponse();
        r.setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse body = ErrorResponseBuilder
                .from(Status.fromStatusCode(status), description)
                .buildEntity();

        try {
            return getMapper().writeValueAsString(body);
        } catch (JsonProcessingException jpe) {
            // Well, at least... return something?
            return "{\"error\":\"internal_server_error\","
                    + "\"error_description\":\"Internal Server Error\"}";
        }
    }
}
