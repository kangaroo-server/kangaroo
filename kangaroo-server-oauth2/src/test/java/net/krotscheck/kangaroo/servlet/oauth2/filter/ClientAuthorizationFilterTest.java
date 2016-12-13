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

package net.krotscheck.kangaroo.servlet.oauth2.filter;

import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.AccessDeniedException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidClientException;
import net.krotscheck.kangaroo.common.hibernate.HibernateFeature;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.servlet.oauth2.annotation.OAuthFilterChain;
import net.krotscheck.kangaroo.servlet.oauth2.factory.CredentialsFactory.Credentials;
import net.krotscheck.kangaroo.test.DContainerTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import javax.ws.rs.container.ContainerRequestContext;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for the client authorization filter.
 *
 * @author Michael Krotscheck
 */
public final class ClientAuthorizationFilterTest extends DContainerTest {

    /**
     * Test cpplication.
     */
    private Application application;

    /**
     * A test private client.
     */
    private Client privateClient;

    /**
     * The test public client.
     */
    private Client publicClient;

    /**
     * The current session.
     */
    private Session session;

    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(ConfigurationFeature.class);
        config.register(HibernateFeature.class);

        return config;
    }

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<EnvironmentBuilder> fixtures() {
        return null;
    }

    /**
     * Setup our test data.
     */
    @Before
    public void setup() {
        session = getSession();

        application = new Application();
        application.setName("Test Application");

        privateClient = new Client();
        privateClient.setApplication(application);
        privateClient.setName("Private");
        privateClient.setType(ClientType.AuthorizationGrant);
        privateClient.setClientSecret("valid_secret");

        publicClient = new Client();
        publicClient.setApplication(application);
        publicClient.setName("Public");
        publicClient.setType(ClientType.AuthorizationGrant);

        Transaction t = session.beginTransaction();
        session.save(application);
        session.save(privateClient);
        session.save(publicClient);
        t.commit();
    }

    /**
     * Clean up entities.
     */
    @After
    public void teardown() {
        Transaction t = session.beginTransaction();
        session.delete(privateClient);
        session.delete(publicClient);
        session.delete(application);
        t.commit();

        privateClient = null;
        publicClient = null;
        application = null;
        session = null;
    }

    /**
     * Assert that this filter is a part of the OAuth chain.
     */
    @Test
    public void testPartOfOAuthChain() {
        OAuthFilterChain annotation = ClientAuthorizationFilter.class
                .getAnnotation(OAuthFilterChain.class);
        Assert.assertNotNull(annotation);
    }

    /**
     * Assert that valid credentials for a public client work.
     *
     * @throws Exception A processing exception.
     */
    @Test
    public void testValidPublicCredentials() throws Exception {
        Credentials c = new Credentials(publicClient.getId().toString(), null);
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that valid credentials for a public client work.
     *
     * @throws Exception A processing exception.
     */
    @Test
    public void testValidPrivateCredentials() throws Exception {
        Credentials c = new Credentials(privateClient.getId().toString(),
                privateClient.getClientSecret());
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that invalid credentials result in an error.
     *
     * @throws Exception A processing exception.
     */
    @Test(expected = AccessDeniedException.class)
    public void testInvalidCredentials() throws Exception {
        Credentials c = new Credentials(privateClient.getId().toString(),
                "invalid");
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that valid credentials not backed by the DB fail.
     *
     * @throws Exception A processing exception.
     */
    @Test(expected = InvalidClientException.class)
    public void testCredentialsNoClient() throws Exception {
        Credentials c = new Credentials(UUID.randomUUID().toString(),
                "invalid");
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that a private client with no password fails.
     *
     * @throws Exception A processing exception.
     */
    @Test(expected = AccessDeniedException.class)
    public void testPrivateClientNoPassword() throws Exception {
        Credentials c = new Credentials(privateClient.getId().toString(),
                null);
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that a private client with a bad password fails.
     *
     * @throws Exception A processing exception.
     */
    @Test(expected = AccessDeniedException.class)
    public void testPrivateClientBadPassword() throws Exception {
        Credentials c = new Credentials(privateClient.getId().toString(),
                "badPassword");
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that a private client with an empty password fails.
     *
     * @throws Exception A processing exception.
     */
    @Test(expected = AccessDeniedException.class)
    public void testPrivateClientEmptyPassword() throws Exception {
        Credentials c = new Credentials(privateClient.getId().toString(), "");
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that a public client with a password fails.
     *
     * @throws Exception A processing exception.
     */
    @Test(expected = AccessDeniedException.class)
    public void testPublicClientPassword() throws Exception {
        Credentials c = new Credentials(publicClient.getId().toString(),
                "badPassword");
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }

    /**
     * Assert that a public client with a password fails.
     *
     * @throws Exception A processing exception.
     */
    @Test(expected = InvalidClientException.class)
    public void testEmptyCredentials() throws Exception {
        Credentials c = new Credentials();
        ClientAuthorizationFilter filter =
                new ClientAuthorizationFilter(() -> c, () -> session);

        filter.filter(mock(ContainerRequestContext.class));
    }
}
