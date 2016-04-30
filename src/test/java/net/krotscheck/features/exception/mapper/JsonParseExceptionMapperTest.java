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

package net.krotscheck.features.exception.mapper;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import net.krotscheck.features.exception.ErrorResponseBuilder.ErrorResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;

/**
 * Test that jersey exceptions are caught and rewritten into appropriate
 * responses.
 *
 * @author Michael Krotscheck
 */
public final class JsonParseExceptionMapperTest {

    /**
     * Test converting to a response.
     */
    @Test
    public void testToResponse() {
        JsonParseExceptionMapper mapper = new JsonParseExceptionMapper();
        JsonParseException jpe = new JsonParseException("foo",
                mock(JsonLocation.class));

        Response r = mapper.toResponse(jpe);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(400, r.getStatus());
        Assert.assertEquals(400, er.getHttpStatus());
        Assert.assertEquals("", er.getRedirectUrl());
    }
}
