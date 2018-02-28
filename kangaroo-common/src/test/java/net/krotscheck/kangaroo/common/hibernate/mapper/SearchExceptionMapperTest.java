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
import org.hibernate.search.exception.EmptyQueryException;
import org.hibernate.search.exception.SearchException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import static net.krotscheck.kangaroo.test.jersey.BinderAssertion.assertBinderContains;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the search exception mapper.
 *
 * @author Michael Krotscheck
 */
public final class SearchExceptionMapperTest {

    /**
     * Assert that an usual search exceptions map to a 500.
     */
    @Test
    public void testToResponse() {
        SearchExceptionMapper mapper = new SearchExceptionMapper();
        SearchException e = new SearchException("test");

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                r.getStatus());
        assertEquals(Status.INTERNAL_SERVER_ERROR, er.getHttpStatus());
        assertEquals("Internal Server Error", er.getErrorDescription());
    }

    /**
     * Assert that an empty query exception maps to a 400.
     */
    @Test
    public void testEmptyQueryResponse() {
        SearchExceptionMapper mapper = new SearchExceptionMapper();
        EmptyQueryException e = new EmptyQueryException();

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                r.getStatus());
        assertEquals(Status.BAD_REQUEST, er.getHttpStatus());
        assertEquals("HTTP 400 Bad Request", er.getErrorDescription());
    }

    /**
     * Assert that we can inject values using this binder.
     */
    @Test
    public void testBinder() {
        assertBinderContains(new SearchExceptionMapper.Binder(),
                ExceptionMapper.class);
    }
}
