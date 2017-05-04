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

import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * Assert that HttpForbiddenException has the appropriate values.
 *
 * @author Michael Krotscheck
 */
public final class HttpUnauthorizedExceptionTest {

    /**
     * Test default values.
     */
    @Test
    public void testBasicExceptions() {
        HttpUnauthorizedException e = new HttpUnauthorizedException();

        Assert.assertEquals(Status.UNAUTHORIZED, e.getHttpStatus());
        Assert.assertEquals("Unauthorized", e.getMessage());
        Assert.assertEquals("unauthorized", e.getErrorCode());
        Assert.assertEquals(null, e.getRedirect());
    }

    /**
     * Test redirect values.
     */
    @Test
    public void testRedirectExceptions() {
        URI redirect = UriBuilder.fromPath("http://www.example.com").build();
        HttpUnauthorizedException e = new HttpUnauthorizedException(redirect);

        Assert.assertEquals(Status.UNAUTHORIZED, e.getHttpStatus());
        Assert.assertEquals("Unauthorized", e.getMessage());
        Assert.assertEquals("unauthorized", e.getErrorCode());
        Assert.assertEquals(redirect, e.getRedirect());
    }
}
