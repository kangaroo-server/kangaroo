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

package net.krotscheck.kangaroo.authz.oauth2.authn.factory;

import net.krotscheck.kangaroo.authz.oauth2.authn.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Provider;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.util.function.Supplier;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the credentials factory.
 *
 * @author Michael Krotscheck
 */
public final class CredentialsFactoryTest {

    /**
     * Build a test context for this unit test.
     *
     * @param authHeader   Create a new auth header.
     * @param method       The HTTP Method
     * @param clientId     The client ID, or null.
     * @param clientSecret The client secret, or null.
     * @return A mock container request context.
     */
    private Provider<ContainerRequest> buildTestContext(
            final String authHeader,
            final String method,
            final String clientId,
            final String clientSecret) {

        UriBuilder pathBuilder = UriBuilder.fromPath("http://example.com/");
        if (method.equals(HttpMethod.GET)) {
            pathBuilder.queryParam("foo", "bar"); // Inject noise.
            if (clientId != null) {
                pathBuilder.queryParam("client_id", clientId);
            }
            if (clientSecret != null) {
                pathBuilder.queryParam("client_secret", clientSecret);
            }
        }

        ContainerRequest request = mock(ContainerRequest.class);

        doReturn(method).when(request).getMethod();
        doReturn(pathBuilder.build()).when(request).getRequestUri();
        if (authHeader != null) {
            doReturn(authHeader).when(request)
                    .getHeaderString(HttpHeaders.AUTHORIZATION);
        }

        // Additional options for POST requests.
        if (HttpMethod.POST.equals(method)) {
            // Create a mock form data object.
            Form form = new Form();
            if (clientId != null) {
                form.param("client_id", clientId);
            }
            if (clientSecret != null) {
                form.param("client_secret", clientSecret);
            }

            doReturn(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    .when(request).getMediaType();
            doReturn(form).when(request).readEntity(Form.class);
            doReturn(true).when(request).hasEntity();
        }

        return () -> request;
    }

    /**
     * Make sure that the EMPTY credentials is not valid.
     */
    @Test
    public void testEmptyDefault() {
        Assert.assertFalse(CredentialsFactory.EMPTY.isValid());
    }

    /**
     * Assert that the generic interface works as expected.
     *
     * @throws Exception Should not be thrown.
     * @see <a href="https://sourceforge.net/p/cobertura/bugs/92/">https://sourceforge.net/p/cobertura/bugs/92/</a>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testGenericInterface() throws Exception {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId,
                                "password"),
                        HttpMethod.GET,
                        IdUtil.toString(clientId),
                        null);

        // Intentionally using the generic untyped interface here.
        Supplier factory = new CredentialsFactory(provider);
        Object instance = factory.get();
        Assert.assertNotNull(instance);
    }

    /**
     * Assert that a valid auth header is found.
     */
    @Test
    public void testValidAuthHeader() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId,
                                "password"),
                        HttpMethod.GET,
                        IdUtil.toString(clientId),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(clientId, creds.getLogin());
        Assert.assertEquals("password", creds.getPassword());
        Assert.assertTrue(creds.isValid());
    }

    /**
     * Assert that an auth header with no password is not parsed.
     */
    @Test
    public void testAuthHeaderNoPassword() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId, ""),
                        HttpMethod.GET,
                        IdUtil.toString(clientId),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(null, creds.getLogin());
        Assert.assertEquals(null, creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that a malformed auth header is not parsed.
     */
    @Test
    public void testAuthHeaderMalformed() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        "invalid_auth_header",
                        HttpMethod.GET,
                        IdUtil.toString(clientId),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(null, creds.getLogin());
        Assert.assertEquals(null, creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that an auth header that does not conform to the BigInteger spec fails.
     */
    @Test
    public void testAuthHeaderNoByte() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic("not_a_BigInteger", "password"),
                        HttpMethod.GET,
                        IdUtil.toString(clientId),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(null, creds.getLogin());
        Assert.assertEquals(null, creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that post credentials are parsed.
     */
    @Test
    public void testPostValid() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.POST,
                        IdUtil.toString(clientId),
                        "password");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(clientId, creds.getLogin());
        Assert.assertEquals("password", creds.getPassword());
        Assert.assertTrue(creds.isValid());
    }

    /**
     * Assert that post without credentials fails.
     */
    @Test
    public void testPostNoData() {
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.POST,
                        null,
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that post with an empty secret passes.
     */
    @Test
    public void testPostEmptySecret() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.POST,
                        IdUtil.toString(clientId),
                        "");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(clientId, creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertTrue(creds.isValid());
    }

    /**
     * Assert that post credentials without a secret are parsed.
     */
    @Test
    public void testPostNoSecret() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.POST,
                        IdUtil.toString(clientId),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(clientId, creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertTrue(creds.isValid());
    }

    /**
     * Assert that post credentials with no id fail.
     */
    @Test
    public void testPostNoId() {
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.POST,
                        null,
                        "password");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that post credentials with a malformed client BigInteger fail.
     */
    @Test
    public void testPostNoByte() {
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.POST,
                        "not_a_BigInteger",
                        "password");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that GET credentials are found.
     */
    @Test
    public void testGetValid() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.GET,
                        IdUtil.toString(clientId),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertEquals(clientId, creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertTrue(creds.isValid());
    }

    /**
     * Assert that GET credentials with no client_id are not found.
     */
    @Test
    public void testGetNoId() {
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.GET,
                        null,
                        "foo");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert a secret in the GET request fails.
     */
    @Test
    public void testGetIgnoreSecret() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.GET,
                        IdUtil.toString(clientId),
                        "password");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that no credentials are found if the keys are empty.
     */
    @Test
    public void testGetEmptyId() {
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.GET,
                        "",
                        "");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that GET credentials with a malformed BigInteger is not found.
     */
    @Test
    public void testGetNoByte() {
        Provider<ContainerRequest> provider =
                buildTestContext(
                        null,
                        HttpMethod.GET,
                        "not_a_BigInteger",
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that POST + Auth header with mismatched client_id's are not
     * found.
     */
    @Test
    public void testPostMismatchClientId() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId,
                                "password"),
                        HttpMethod.POST,
                        IdUtil.toString(IdUtil.next()),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that trying to auth twice (POST + Header) fails.
     */
    @Test
    public void testPostDoubleAuth() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId,
                                "password"),
                        HttpMethod.POST,
                        IdUtil.toString(IdUtil.next()),
                        "password");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that mismatching client ID's in GET requests fail.
     */
    @Test
    public void testGetMismatchClientId() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId,
                                "password"),
                        HttpMethod.GET,
                        IdUtil.toString(IdUtil.next()),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that trying to auth twice via GET request fails.
     */
    @Test
    public void testGetDoubleAuth() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId,
                                "password"),
                        HttpMethod.GET,
                        IdUtil.toString(IdUtil.next()),
                        "password");

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }

    /**
     * Assert that neither post nor delete get processed.
     */
    @Test
    public void testNonGetPost() {
        BigInteger clientId = IdUtil.next();
        Provider<ContainerRequest> provider =
                buildTestContext(
                        HttpUtil.authHeaderBasic(clientId,
                                "password"),
                        HttpMethod.DELETE,
                        IdUtil.toString(IdUtil.next()),
                        null);

        CredentialsFactory factory = new CredentialsFactory(provider);
        Credentials creds = factory.get();

        Assert.assertNull(creds.getLogin());
        Assert.assertNull(creds.getPassword());
        Assert.assertFalse(creds.isValid());
    }
}
