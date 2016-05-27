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

package net.krotscheck.api.oauth.util;

import net.krotscheck.features.database.util.SortUtil;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Test for the validation utilities.
 *
 * @author Michael Krotscheck
 */
public final class ValidationUtilTest {

    /**
     * Assert that the header is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = ValidationUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Check simple valid redirect.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testValidateRedirect() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com"));
        testSet.add(new URI("http://two.example.com"));

        URI result = ValidationUtil.validateRedirect("http://one.example.com",
                testSet);
        Assert.assertEquals("http://one.example.com", result.toString());
    }

    /**
     * Check simple valid redirect.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testInvalidRedirect() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com"));
        testSet.add(new URI("http://two.example.com"));

        URI result = ValidationUtil.validateRedirect("http://three.example.com",
                testSet);
        Assert.assertNull(result);
    }

    /**
     * Test fallback to default.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testValidateRedirectDefault() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://two.example.com"));

        URI result = ValidationUtil.validateRedirect("",
                testSet);
        Assert.assertEquals("http://two.example.com", result.toString());
    }

    /**
     * Test fallback to default if there's too many.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testValidateRedirectDefaultTooMany() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com"));
        testSet.add(new URI("http://two.example.com"));

        URI result = ValidationUtil.validateRedirect("",
                testSet);
        Assert.assertNull(result);
    }

    /**
     * Test redirect check with none in the input set.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testValidateRedirectNoOptions() throws Exception {
        URI result = ValidationUtil.validateRedirect("http://two.example.com",
                new HashSet<>());
        Assert.assertNull(result);
    }

    /**
     * Test that a malformed input URI results in a null response.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testValidateRedirectMalformed() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://two.example.com"));

        URI result = ValidationUtil.validateRedirect("http:\\",
                testSet);
        Assert.assertNull(result);
    }
}
