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

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

/**
 * Assert that HttpNotFoundException has the appropriate values.
 *
 * @author Michael Krotscheck
 */
public class HttpRedirectExceptionTest {

    @Test
    public void testHttpStatusException() {
        HttpRedirectException e =
                new HttpRedirectException("http://localhost/");
        Assert.assertTrue(e instanceof HttpStatusException);
    }

    @Test
    public void testMalformedConstructor() {
        HttpRedirectException e = new HttpRedirectException("foo");

        Assert.assertEquals(303, e.getHttpStatus());
        Assert.assertEquals("See Other", e.getMessage());
        Assert.assertNull(e.getRedirectUrl());
    }

    @Test
    public void testStringConstructor() {
        HttpRedirectException e =
                new HttpRedirectException("http://example.com");

        Assert.assertEquals(303,
                e.getHttpStatus());
        Assert.assertEquals("See Other",
                e.getMessage());
        Assert.assertEquals("http://example.com",
                e.getRedirectUrl().toString());
    }

    @Test
    public void testUrlConstructor() throws Exception {
        URL url = new URL("http://test.example.com");
        HttpRedirectException e = new HttpRedirectException(url);

        Assert.assertEquals(303, e.getHttpStatus());
        Assert.assertEquals("See Other", e.getMessage());
        Assert.assertEquals("http://test.example.com",
                e.getRedirectUrl().toString());
    }
}