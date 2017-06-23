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

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.net.URI;

/**
 * Unit tests for our common application exception.
 *
 * @author Michael Krotscheck
 */
public class KangarooExceptionTest {

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

        ErrorResponse response = (ErrorResponse) e.getResponse().getEntity();

        Assert.assertEquals(TestError.CODE.getError(),
                response.getError());
        Assert.assertEquals(TestError.CODE.getErrorDescription(),
                response.getErrorDescription());
        Assert.assertEquals(TestError.CODE.getHttpStatus(),
                response.getHttpStatus());
    }

    /**
     * Assert creating code and redirect.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testWithCodeAndRedirect() throws Exception {
        URI redirect = new URI("http://redirect.example.com/");
        KangarooException e = new TestError(redirect);

        Assert.assertEquals(Status.FOUND.getStatusCode(),
                e.getResponse().getStatus());

        Assert.assertEquals(redirect.getScheme(),
                e.getResponse().getLocation().getScheme());
        Assert.assertEquals(redirect.getHost(),
                e.getResponse().getLocation().getHost());
        Assert.assertEquals(redirect.getPort(),
                e.getResponse().getLocation().getPort());
        Assert.assertEquals(redirect.getPath(),
                e.getResponse().getLocation().getPath());
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

        /**
         * Create a new exception with the specified redirect.
         *
         * @param redirect The redirect.
         */
        protected TestError(final URI redirect) {
            super(CODE, redirect);
        }
    }
}