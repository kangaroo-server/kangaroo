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

import net.krotscheck.api.oauth.exception.exception.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.features.database.entity.ApplicationScope;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Test for the validation utilities.
 *
 * @author Michael Krotscheck
 */
public final class ValidationUtilTest {

    private SortedMap<String, ApplicationScope> validScopes;

    @Before
    public void setupTest() {
        validScopes = new TreeMap<>();
        validScopes.put("debug", new ApplicationScope());
        validScopes.put("debug1", new ApplicationScope());
    }

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

    /**
     * Assert that a simple scope validation request works.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test
    public void testValidScopeString() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.validateScope("debug1", validScopes);
        Assert.assertEquals(1, scopes.size());
    }

    /**
     * Assert that a simple scope validation request works.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test
    public void testValidScopeArray() throws Exception {
        SortedMap<String, ApplicationScope> scopes = ValidationUtil
                .validateScope(new String[]{"debug1"}, validScopes);
        Assert.assertEquals(1, scopes.size());
    }

    /**
     * Assert that we cannot use an invalid scope.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test(expected = InvalidScopeException.class)
    public void testInvalidScope() throws Exception {
        SortedMap<String, ApplicationScope> scopes = ValidationUtil
                .validateScope(new String[]{"invalid"}, validScopes);
    }

    /**
     * Assert that null scope string passes.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test
    public void testNullScopeString() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.validateScope((String) null, validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that empty scope string passes.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test
    public void testEmptyScopeString() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.validateScope("", validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that null scope array passes.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test
    public void testNullScopeArray() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.validateScope((String[]) null, validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that empty scope array passes.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test
    public void testEmptyScopeArray() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.validateScope(new String[]{}, validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that null valid scopes passes.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test(expected = InvalidScopeException.class)
    public void testNullValidScopes() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.validateScope(new String[]{"debug1"}, null);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that empty valid scopes fails.
     *
     * @throws Exception Should be thrown when the validation fails.
     */
    @Test(expected = InvalidScopeException.class)
    public void testEmptyValidScopes() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.validateScope(new String[]{"debug1"},
                        new TreeMap<>());
        Assert.assertEquals(0, scopes.size());
    }
}
