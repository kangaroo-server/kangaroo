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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test the kangaroo error page generator.
 *
 * @author Michael Krotscheck
 */
public final class KangarooErrorPageGeneratorTest {

    /**
     * Internal test mapper.
     */
    private final static ObjectMapper MAPPER = new ObjectMapperFactory().get();

    /**
     * Assert that a basic error is converted to an ErrorResponse instance.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertBasicError() throws Exception {
        KangarooErrorPageGenerator generator = new KangarooErrorPageGenerator();
        Request r = mock(Request.class);
        Response response = mock(Response.class);
        doReturn(response)
                .when(r)
                .getResponse();

        String body = generator.generate(r, 404, "Not Found",
                "Not Found", null);
        ErrorResponse decoded = MAPPER.readValue(body, ErrorResponse.class);
        assertEquals("not_found", decoded.getError());
        assertEquals("Not Found", decoded.getErrorDescription());
        verify(response, times(1))
                .setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Assert that a mapping error returns an internal response error.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertMappingError() throws Exception {
        KangarooErrorPageGenerator generator = new KangarooErrorPageGenerator();
        KangarooErrorPageGenerator generatorSpy = spy(generator);

        Request r = mock(Request.class);
        Response response = mock(Response.class);
        doReturn(response)
                .when(r)
                .getResponse();

        doThrow(JsonParseException.class)
                .when(generatorSpy)
                .getMapper();

        String body = generatorSpy.generate(r, 404, "Not Found",
                "Not Found", null);
        ErrorResponse decoded = MAPPER.readValue(body, ErrorResponse.class);
        assertEquals("internal_server_error", decoded.getError());
        assertEquals("Internal Server Error", decoded.getErrorDescription());

        verify(response, times(1))
                .setContentType(MediaType.APPLICATION_JSON);
    }
}
