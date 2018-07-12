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

package net.krotscheck.kangaroo.common.exception;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for the system maintenance exception.
 *
 * @author Michael Krotscheck
 */
public final class SystemMaintenanceExceptionTest {

    /**
     * Test the type.
     */
    @Test
    public void testExtendsKangarooException() {
        SystemMaintenanceException e = new SystemMaintenanceException();
        assertTrue(e instanceof KangarooException);
    }

    /**
     * Test the message.
     */
    @Test
    public void testCustomMessage() {
        SystemMaintenanceException e = new SystemMaintenanceException();
        assertEquals(e.getMessage(),
                SystemMaintenanceException.CODE.getError());
    }

    /**
     * Test serialization.
     */
    @Test
    public void testSerialization() {
        ErrorResponse e = ErrorResponseBuilder
                .from(new SystemMaintenanceException())
                .buildEntity();

        assertEquals(SystemMaintenanceException.CODE.getErrorDescription(),
                e.getErrorDescription());
        assertEquals(Status.SERVICE_UNAVAILABLE, e.getHttpStatus());
        assertEquals(SystemMaintenanceException.CODE.getError(), e.getError());
    }

}
