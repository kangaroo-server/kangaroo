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

package net.krotscheck.features.exception.exception;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

/**
 * Assert that HttpStatusException is a good basic constructor.
 *
 * @author Michael Krotscheck
 */
public final class HttpStatusExceptionTest {

    /**
     * Identity Test.
     */
    @Test
    public void testHttpStatusException() {
        HttpForbiddenException e = new HttpForbiddenException();
        Assert.assertTrue(e instanceof HttpStatusException);
    }

    /**
     * Test construction with a status code.
     */
    @Test
    public void testHttpStatusConstruction() {
        HttpStatusException e =
                new HttpStatusException(HttpStatus.SC_OK);

        Assert.assertEquals(200, e.getHttpStatus());
        Assert.assertEquals("OK", e.getMessage());
    }

    /**
     * Test construction with a status code and a message.
     */
    @Test
    public void testHttpStatusMessageConstructor() {
        HttpStatusException e =
                new HttpStatusException(HttpStatus.SC_OK, "Message");

        Assert.assertEquals(200, e.getHttpStatus());
        Assert.assertEquals("Message", e.getMessage());
    }
}
