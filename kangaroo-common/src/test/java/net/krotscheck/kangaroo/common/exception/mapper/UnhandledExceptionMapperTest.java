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

package net.krotscheck.kangaroo.common.exception.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.exception.mapper.UnhandledExceptionMapper;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * Test that jersey exceptions are caught and rewritten into appropriate
 * responses.
 *
 * @author Michael Krotscheck
 */
public final class UnhandledExceptionMapperTest {

    /**
     * Test converting to a response.
     */
    @Test
    public void testToResponse() {
        UnhandledExceptionMapper mapper = new UnhandledExceptionMapper();
        Exception e = new Exception("test");

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(500, r.getStatus());
        Assert.assertEquals(500, er.getHttpStatus());
        Assert.assertEquals("Internal Server Error", er.getErrorDescription());
        Assert.assertNull(er.getRedirectUrl());
    }
}
