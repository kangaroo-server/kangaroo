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
import org.hibernate.PropertyValueException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import static net.krotscheck.kangaroo.test.jersey.BinderAssertion.assertBinderContains;
import static org.junit.Assert.assertEquals;

/**
 * Smoke test for the Property Value Exception Mapper.
 *
 * @author Michael Krotscheck
 */
public final class PropertyValueExceptionMapperTest {

    /**
     * Assert that an usual search exceptions map to a 500.
     */
    @Test
    public void testToResponse() {
        PropertyValueExceptionMapper mapper =
                new PropertyValueExceptionMapper();
        PropertyValueException e =
                new PropertyValueException("test", "thing", "name");

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertEquals(Status.BAD_REQUEST, er.getHttpStatus());
        assertEquals("Property \"name\" is invalid.",
                er.getErrorDescription());
    }

    /**
     * Assert that we can inject values using this binder.
     */
    @Test
    public void testBinder() {
        assertBinderContains(new PropertyValueExceptionMapper.Binder(),
                ExceptionMapper.class);
    }
}
