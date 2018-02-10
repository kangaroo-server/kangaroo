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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import java.sql.SQLException;

import static net.krotscheck.kangaroo.test.jersey.BinderAssertion.assertBinderContains;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the hibernate exception mapper. exception mapper.
 *
 * @author Michael Krotscheck
 */
public final class PersistenceExceptionMapperTest {

    /**
     * Assert that an exception with a cause is properly passed on.
     */
    @Test
    public void testToResponse() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "Test Exception", new SQLException(), "constraintName"
        );
        PersistenceException e = new PersistenceException("Test", cause);

        PersistenceExceptionMapper mapper = new PersistenceExceptionMapper();

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        assertEquals(Status.CONFLICT.getStatusCode(), r.getStatus());
        assertEquals(Status.CONFLICT, er.getHttpStatus());
        assertEquals("Conflict", er.getErrorDescription());
    }

    /**
     * Assert that an exception without a cause is mapped to a 500.
     */
    @Test
    public void testToResponseNoCause() {
        PersistenceExceptionMapper mapper = new PersistenceExceptionMapper();
        PersistenceException e = new PersistenceException("test");

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                r.getStatus());
        assertEquals(Status.INTERNAL_SERVER_ERROR, er.getHttpStatus());
        assertEquals("Internal Server Error", er.getErrorDescription());
    }

    /**
     * Assert that we can inject values using this binder.
     */
    @Test
    public void testBinder() {
        assertBinderContains(new PersistenceExceptionMapper.Binder(),
                ExceptionMapper.class);
    }
}
