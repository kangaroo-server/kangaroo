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

package net.krotscheck.kangaroo.common.hibernate.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.hibernate.QueryException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;

/**
 * Query exception mapping.
 */
public final class QueryExceptionMapperTest {

    /**
     * Assert that an usual search exceptions map to a 400.
     */
    @Test
    public void testToResponse() {
        QueryExceptionMapper mapper = new QueryExceptionMapper();
        QueryException e = new QueryException("test");

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertEquals(Status.BAD_REQUEST, er.getHttpStatus());
        assertEquals("Bad Request", er.getErrorDescription());
    }

}
