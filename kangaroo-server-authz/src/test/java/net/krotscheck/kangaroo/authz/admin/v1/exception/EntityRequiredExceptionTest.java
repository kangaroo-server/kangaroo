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

package net.krotscheck.kangaroo.authz.admin.v1.exception;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the EntityRequired exception.
 *
 * @author Michael Krotscheck
 */
public final class EntityRequiredExceptionTest {

    /**
     * Test the type.
     */
    @Test
    public void testExtendsBadRequest() {
        EntityRequiredException e = new EntityRequiredException();
        assertTrue(e instanceof BadRequestException);
    }

    /**
     * Test the message.
     */
    @Test
    public void testCustomMessage() {
        EntityRequiredException e = new EntityRequiredException();
        assertEquals(e.getMessage(), EntityRequiredException.MESSAGE);
    }

    /**
     * Test serialization.
     */
    @Test
    public void testSerialization() {
        ErrorResponse e = ErrorResponseBuilder
                .from(new EntityRequiredException())
                .buildEntity();

        assertEquals(EntityRequiredException.MESSAGE, e.getErrorDescription());
        assertEquals(Status.BAD_REQUEST, e.getHttpStatus());
        assertEquals("bad_request", e.getError());
    }
}
