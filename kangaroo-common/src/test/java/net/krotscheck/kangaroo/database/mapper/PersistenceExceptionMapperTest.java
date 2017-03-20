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

package net.krotscheck.kangaroo.database.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.apache.http.HttpStatus;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

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

        Assert.assertEquals(HttpStatus.SC_CONFLICT, r.getStatus());
        Assert.assertEquals(HttpStatus.SC_CONFLICT, er.getHttpStatus());
        Assert.assertEquals("Conflict", er.getErrorDescription());
        Assert.assertNull(er.getRedirectUrl());
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

        Assert.assertEquals(500, r.getStatus());
        Assert.assertEquals(500, er.getHttpStatus());
        Assert.assertEquals("Internal Server Error", er.getErrorDescription());
        Assert.assertNull(er.getRedirectUrl());
    }
}
