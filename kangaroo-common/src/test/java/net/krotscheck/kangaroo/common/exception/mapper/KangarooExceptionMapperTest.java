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

package net.krotscheck.kangaroo.common.exception.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Unit tests for mapping kangaroo exceptions.
 *
 * @author Michael Krotscheck
 */
public class KangarooExceptionMapperTest {

    /**
     * Test converting to a response.
     */
    @Test
    public void testToResponse() {
        KangarooExceptionMapper mapper = new KangarooExceptionMapper();
        TestError jpe = new TestError();

        Response r = mapper.toResponse(jpe);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), r.getStatus());
        Assert.assertEquals(Status.FORBIDDEN, er.getHttpStatus());
        Assert.assertEquals("test_error", er.getError());
        Assert.assertEquals("Test Error", er.getErrorDescription());
    }

    /**
     * Test implementation of the KangarooException.
     */
    public static final class TestError extends KangarooException {

        /**
         * The error code.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.FORBIDDEN,
                "test_error",
                "Test Error"
        );

        /**
         * Create a new exception.
         */
        protected TestError() {
            super(CODE);
        }
    }
}
