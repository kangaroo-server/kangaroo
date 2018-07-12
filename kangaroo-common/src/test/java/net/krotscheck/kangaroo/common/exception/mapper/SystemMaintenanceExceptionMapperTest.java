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

package net.krotscheck.kangaroo.common.exception.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.exception.SystemMaintenanceException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the maintenance exception mapper.
 *
 * @author Michael Krotscheck
 */
public final class SystemMaintenanceExceptionMapperTest {

    /**
     * Test converting to a response.
     */
    @Test
    public void testToResponse() {
        SystemMaintenanceExceptionMapper mapper
                = new SystemMaintenanceExceptionMapper();
        SystemMaintenanceException jpe = new SystemMaintenanceException();

        Response r = mapper.toResponse(jpe);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());
        assertEquals(Status.SERVICE_UNAVAILABLE, er.getHttpStatus());
        assertEquals(SystemMaintenanceException.CODE.getError(),
                er.getError());
        assertEquals(SystemMaintenanceException.CODE.getErrorDescription(),
                er.getErrorDescription());
    }

}
