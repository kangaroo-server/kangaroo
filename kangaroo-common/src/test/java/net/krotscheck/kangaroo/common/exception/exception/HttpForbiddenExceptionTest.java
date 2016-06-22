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

package net.krotscheck.kangaroo.common.exception.exception;

import net.krotscheck.kangaroo.common.exception.exception.HttpForbiddenException;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;

/**
 * Assert that HttpForbiddenException has the appropriate values.
 *
 * @author Michael Krotscheck
 */
public final class HttpForbiddenExceptionTest {

    /**
     * Identity Test.
     */
    @Test
    public void testHttpStatusException() {
        HttpForbiddenException e = new HttpForbiddenException();
        Assert.assertTrue(e instanceof HttpStatusException);
    }

    /**
     * Test default values.
     */
    @Test
    public void testBasicExceptions() {
        HttpForbiddenException e = new HttpForbiddenException();

        Assert.assertEquals(403, e.getHttpStatus());
        Assert.assertEquals("Forbidden", e.getMessage());
        Assert.assertEquals("forbidden", e.getErrorCode());
        Assert.assertEquals(null, e.getRedirect());
    }

    /**
     * Test redirect values.
     */
    @Test
    public void testRedirectExceptions() {
        URI redirect = UriBuilder.fromPath("http://www.example.com").build();
        HttpForbiddenException e = new HttpForbiddenException(redirect);

        Assert.assertEquals(403, e.getHttpStatus());
        Assert.assertEquals("Forbidden", e.getMessage());
        Assert.assertEquals("forbidden", e.getErrorCode());
        Assert.assertEquals(redirect, e.getRedirect());
    }
}
