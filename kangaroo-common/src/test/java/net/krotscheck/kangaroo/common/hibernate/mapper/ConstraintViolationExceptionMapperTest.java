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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashSet;
import java.util.Set;

/**
 * Test the constraint violation exception mapping.
 *
 * @author Michael Krotscheck
 */
public final class ConstraintViolationExceptionMapperTest {

    /**
     * Assert that an usual search exceptions map to a 500.
     */
    @Test
    public void testToResponse() {
        Set<ConstraintViolation<Application>> violations = new HashSet<>();
        ConstraintViolation m = Mockito.mock(ConstraintViolation.class);
        Mockito.when(m.getMessage()).thenReturn("test 1");
        violations.add(m);

        ConstraintViolationExceptionMapper mapper =
                new ConstraintViolationExceptionMapper();
        ConstraintViolationException e =
                new ConstraintViolationException("message", violations);

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        Assert.assertEquals(Status.BAD_REQUEST, er.getHttpStatus());
        Assert.assertEquals("test 1", er.getErrorDescription());
        Assert.assertNull(er.getRedirectUrl());
    }

}
