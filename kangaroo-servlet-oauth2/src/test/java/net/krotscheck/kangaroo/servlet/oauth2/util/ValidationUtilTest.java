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

package net.krotscheck.kangaroo.servlet.oauth2.util;

import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidRequestException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.UnsupportedResponseType;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Test for the validation utilities.
 *
 * @author Michael Krotscheck
 */
public final class ValidationUtilTest {

    /**
     * List of valid scopes to test against.
     */
    private SortedMap<String, ApplicationScope> validScopes;

    /**
     * List of valid scopes to test against.
     */
    private SortedMap<String, ApplicationScope> emptyScope = new TreeMap<>();

    /**
     * Bootstrap this test.
     */
    @Before
    public void setupTest() {
        validScopes = new TreeMap<>();
        validScopes.put("debug", new ApplicationScope());
        validScopes.put("debug1", new ApplicationScope());
    }

    /**
     * Assert that the header is private.
     *
     * @throws Exception Thrown if validation fails.
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
     * @throws Exception Thrown if validation fails.
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
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testInvalidRedirectHost() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com"));
        testSet.add(new URI("http://two.example.com"));

        ValidationUtil.validateRedirect("http://three.example.com", testSet);
    }

    /**
     * Check simple valid redirect.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testInvalidRedirectPort() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com:800"));
        testSet.add(new URI("http://two.example.com:800"));

        ValidationUtil.validateRedirect("http://one.example.com:900", testSet);
    }

    /**
     * Check simple valid redirect.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testInvalidRedirectScheme() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("https://one.example.com"));

        ValidationUtil.validateRedirect("http://one.example.com", testSet);
    }

    /**
     * Check simple valid redirect.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testInvalidRedirectPath() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com/foo"));

        ValidationUtil.validateRedirect("http://one.example.com/bar", testSet);
    }

    /**
     * Test fallback to default.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateRedirectDefault() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://two.example.com"));

        URI result = ValidationUtil.validateRedirect("", testSet);
        Assert.assertEquals("http://two.example.com", result.toString());
    }

    /**
     * Test fallback to default if there's too many.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testValidateRedirectDefaultTooMany() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com"));
        testSet.add(new URI("http://two.example.com"));

        ValidationUtil.validateRedirect("", testSet);
    }

    /**
     * Test redirect check with none in the input set.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testValidateRedirectNoOptions() throws Exception {
        ValidationUtil.validateRedirect("http://two.example.com",
                new HashSet<>());
    }

    /**
     * Test that a malformed input URI results in a null response.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testValidateRedirectMalformed() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://two.example.com"));
        ValidationUtil.validateRedirect("http:\\", testSet);
    }

    /**
     * Test that query parameters are considered valid.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateRedirectCustomQueryParams() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com/"));
        testSet.add(new URI("http://two.example.com/"));

        URI test = new URI("http://two.example.com/?foo=bar");

        URI result = ValidationUtil.validateRedirect(test.toString(), testSet);
        Assert.assertEquals(test, result);
    }

    /**
     * Test that additional query parameters are considered valid.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateRedirectAdditionalQueryParams() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com/?foo=bar"));

        URI test = new URI("http://one.example.com/?foo=bar&lol=cat");

        URI result = ValidationUtil.validateRedirect(test.toString(), testSet);
        Assert.assertEquals(test, result);
    }

    /**
     * Test that additional query parameters are considered valid if there's
     * multiple of the same query param in the list.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateRedirectMultipleQueryParams() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com/?foo=bar&foo=cat"));

        URI test = new URI("http://one.example.com/?foo=bar&foo=cat&lol=cat");

        URI result = ValidationUtil.validateRedirect(test.toString(), testSet);
        Assert.assertEquals(test, result);
    }

    /**
     * Test that additional query parameters are considered valid if there's
     * multiple of the same query param in the list.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateRedirectMultipleQueryParamsOverlap()
            throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com/?foo=bar"));

        URI test = new URI("http://one.example.com/?foo=bar&foo=dice&lol=cat");

        URI result = ValidationUtil.validateRedirect(test.toString(), testSet);
        Assert.assertEquals(test, result);
    }

    /**
     * Test that conflicting query parameters are considered invalid.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testValidateRedirectConflictingQueryParams() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com/?foo=bar"));

        URI test = new URI("http://one.example.com/?foo=cat");

        ValidationUtil.validateRedirect(test.toString(), testSet);
    }

    /**
     * Test that additional query parameters are checked against all options.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateRedirectConflictingMultiParams() throws Exception {
        Set<URI> testSet = new HashSet<>();
        testSet.add(new URI("http://one.example.com/?foo=cat"));
        testSet.add(new URI("http://one.example.com/?foo=bar"));

        URI test = new URI("http://one.example.com/?foo=bar");

        URI result = ValidationUtil.validateRedirect(test.toString(), testSet);
        Assert.assertEquals(test, result);
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

    /**
     * Assert that a basic test passes using a request array.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateRequestArray() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope(
                        new String[]{"debug1"}, validScopes, validScopes);
        Assert.assertEquals(1, scopes.size());
    }

    /**
     * Assert that a basic test passes using a request string.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateRequestString() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope(
                        "debug1", validScopes, validScopes);
        Assert.assertEquals(1, scopes.size());
    }

    /**
     * Assert that an empty request string works.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateEmptyRequestString() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope(
                        "", validScopes, validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that a null request string passes.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateNullRequestString() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope((String) null, validScopes,
                        validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that an empty request array works.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateEmptyRequestArray() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope(new String[]{}, validScopes,
                        validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that a null request array fails.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateNullRequestArray() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope((String[]) null, validScopes,
                        validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that an empty original scope works.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateEmptyOriginalScope() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope(new String[]{}, emptyScope,
                        validScopes);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that a null original scope fails.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRevalidateNullOriginalScope() throws Exception {
        ValidationUtil.revalidateScope(new String[]{}, null, validScopes);
    }

    /**
     * Assert that an empty valid scope list works.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateEmptyValidScope() throws Exception {
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope(new String[]{}, validScopes,
                        emptyScope);
        Assert.assertEquals(0, scopes.size());
    }

    /**
     * Assert that a null valid scope list fails.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRevalidateNullValidScope() throws Exception {
        ValidationUtil.revalidateScope(new String[]{"debug"}, validScopes,
                null);
    }

    /**
     * Assert that we cannot escalate scope.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test(expected = InvalidScopeException.class)
    public void testRevalidateCannotEscalateRequestScope() throws Exception {
        SortedMap<String, ApplicationScope> granted = new TreeMap<>();
        granted.put("debug1", new ApplicationScope());
        ValidationUtil.revalidateScope(
                new String[]{"debug", "debug1"}, granted, validScopes);
    }

    /**
     * Assert that shrinking valid scopes pass.
     *
     * @throws Exception Should only be thrown when the validation fails.
     */
    @Test
    public void testRevalidateValidScopesShrank() throws Exception {
        SortedMap<String, ApplicationScope> shrunkScopes = new TreeMap<>();
        shrunkScopes.put("debug1", new ApplicationScope());
        SortedMap<String, ApplicationScope> scopes =
                ValidationUtil.revalidateScope(
                        new String[]{"debug", "debug1"}, validScopes,
                        shrunkScopes);
        Assert.assertEquals(1, scopes.size());
    }

    /**
     * Check simple valid authenticator.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateAuthenticator() throws Exception {
        List<Authenticator> testList = new ArrayList<>();

        Authenticator one = new Authenticator();
        one.setType("one");
        Authenticator two = new Authenticator();
        two.setType("two");
        testList.add(one);
        testList.add(two);

        Authenticator result = ValidationUtil.validateAuthenticator("two",
                testList);
        Assert.assertEquals(result, two);
    }

    /**
     * Check simple valid authenticator.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testInvalidAuthenticator() throws Exception {
        List<Authenticator> testList = new ArrayList<>();

        Authenticator one = new Authenticator();
        one.setType("one");
        Authenticator two = new Authenticator();
        two.setType("two");
        testList.add(one);
        testList.add(two);

        ValidationUtil.validateAuthenticator("three", testList);
    }

    /**
     * Test fallback to default.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test
    public void testValidateAuthenticatorDefault() throws Exception {
        List<Authenticator> testList = new ArrayList<>();

        Authenticator one = new Authenticator();
        one.setType("one");
        testList.add(one);

        Authenticator result = ValidationUtil.validateAuthenticator(null,
                testList);
        Assert.assertEquals(result, one);
    }

    /**
     * Test fallback to default if there's too many.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testValidateAuthenticatorDefaultTooMany() throws Exception {
        List<Authenticator> testList = new ArrayList<>();

        Authenticator one = new Authenticator();
        one.setType("one");
        Authenticator two = new Authenticator();
        two.setType("two");
        testList.add(one);
        testList.add(two);

        ValidationUtil.validateAuthenticator(null, testList);
    }

    /**
     * Test authenticator check with none in the input set.
     *
     * @throws Exception Thrown if validation fails.
     */
    @Test(expected = InvalidRequestException.class)
    public void testValidateAuthenticatorNoOptions() throws Exception {
        List<Authenticator> testList = new ArrayList<>();

        ValidationUtil.validateAuthenticator(null, testList);
    }

    /**
     * Assert that an implicit client can ask for the 'token' response type.
     */
    @Test
    public void testResponseTypeValidImplicitResponseType() {
        Client c = new Client();
        c.setType(ClientType.Implicit);

        ValidationUtil.validateResponseType(c, "token");
        Assert.assertTrue(true);
    }

    /**
     * Assert that an authorization grant client can ask for the 'code'
     * response type.
     */
    @Test
    public void testResponseTypeValidGrantResponseType() {
        Client c = new Client();
        c.setType(ClientType.AuthorizationGrant);

        ValidationUtil.validateResponseType(c, "code");
        Assert.assertTrue(true);
    }

    /**
     * Assert that client/code mismatch on implicit fails.
     */
    @Test(expected = UnsupportedResponseType.class)
    public void testResponseTypeMismatchedImplicitType() {
        Client c = new Client();
        c.setType(ClientType.Implicit);

        ValidationUtil.validateResponseType(c, "code");
    }

    /**
     * Assert that client/code mismatch on authorization grant fails.
     */
    @Test(expected = UnsupportedResponseType.class)
    public void testResponseTypeMismatchedGrantType() {
        Client c = new Client();
        c.setType(ClientType.AuthorizationGrant);

        ValidationUtil.validateResponseType(c, "token");
    }

    /**
     * Assert that some other grant combination fails..
     */
    @Test(expected = UnsupportedResponseType.class)
    public void testResponseTypeBogusType() {
        Client c = new Client();
        c.setType(ClientType.OwnerCredentials);

        ValidationUtil.validateResponseType(c, "code");
    }

    /**
     * Assert that null passed values cause issues as expected.
     */
    @Test(expected = UnsupportedResponseType.class)
    public void testResponseTypeNullResponseType() {
        Client c = new Client();
        c.setType(ClientType.OwnerCredentials);

        ValidationUtil.validateResponseType(c, null);
    }

    /**
     * Assert that null passed values cause issues as expected.
     */
    @Test(expected = UnsupportedResponseType.class)
    public void testResponseTypeNullClient() {
        Client c = new Client();
        c.setType(ClientType.OwnerCredentials);

        ValidationUtil.validateResponseType(null, "code");
    }
}
