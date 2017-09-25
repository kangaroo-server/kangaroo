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

package net.krotscheck.kangaroo.common.exception;

import net.krotscheck.kangaroo.common.exception.KangarooException.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;

/**
 * Unit tests for our common application exception.
 *
 * @author Michael Krotscheck
 */
public class KangarooExceptionTest {

    /**
     * Test the error code class.
     */
    @Test
    public void testErrorCode() {
        ErrorCode testCode = new ErrorCode(Status.FORBIDDEN,
                "code", "description");
        Assert.assertEquals(Status.FORBIDDEN, testCode.getHttpStatus());
        Assert.assertEquals("code", testCode.getError());
        Assert.assertEquals("description", testCode.getErrorDescription());
    }

    /**
     * Assert creating with code, but no redirect.
     */
    @Test
    public void testGetCode() {
        KangarooException e = new TestError();
        Assert.assertSame(TestError.CODE, e.getCode());
    }

    /**
     * Assert creating with code, but no redirect.
     */
    @Test
    public void testPlain() {
        KangarooException e = new TestError();

        Assert.assertEquals(TestError.CODE.getHttpStatus().getStatusCode(),
                e.getResponse().getStatus());
        Assert.assertEquals(e.getCode(), TestError.CODE);
    }

    /**
     * Test implementation of the KangarooException.
     */
    public static final class TestError extends KangarooException {

        /**
         * The error code.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.BAD_REQUEST,
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
