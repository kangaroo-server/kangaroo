/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.oauth2.authn.authn;

import net.krotscheck.kangaroo.authz.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2AuthDynamicFeature;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for authentication via query parameters.
 *
 * @author Michael Krotscheck
 */
public final class O2ClientQueryParameterFilterTest extends ContainerTest {

    /**
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE);

    /**
     * Setup an application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig a = new ResourceConfig();

        // Build a minimal application
        a.register(ConfigurationFeature.class);
        a.register(DatabaseFeature.class);
        a.register(ExceptionFeature.class);
        a.register(new ServletConfigFactory.Binder());
        a.register(new FirstRunContainerLifecycleListener.Binder());

        // Layer in the code under test. Taking the whole feature set here,
        // because it's easier.
        a.register(O2AuthDynamicFeature.class);

        // Add our test resource.
        a.register(O2TestResource.class);

        return a;
    }

    /**
     * Assert that a request with no query parameters fails.
     */
    @Test
    public void testNoParam() {
        Response r = target("/client")
                .request()
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with no client request parameters fails.
     */
    @Test
    public void testNoClientAuthParams() {
        Response r = target("/client")
                .queryParam("hello", "world")
                .queryParam("hello", "kitty")
                .request()
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a malformed client id fails.
     */
    @Test
    public void testMalformedClientId() {
        Response r = target("/client")
                .queryParam("client_id", "not_a_biginteger")
                .request()
                .get();

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a nonexistent client id fails.
     */
    @Test
    public void testNonexistentClientId() {
        Response r = target("/client")
                .queryParam("client_id", IdUtil.toString(IdUtil.next()))
                .request()
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with duplicate client id's fails.
     */
    @Test
    public void testDuplicateClientIds() {
        ApplicationBuilder b = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder();
        Client c1 = b.client(ClientType.Implicit, false)
                .build().getClient();
        Client c2 = b.client(ClientType.Implicit, false)
                .build().getClient();

        Response r = target("/client")
                .queryParam("client_id", IdUtil.toString(c1.getId()))
                .queryParam("client_id", IdUtil.toString(c2.getId()))
                .request()
                .get();

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a private client fails.
     */
    @Test
    public void testValidPrivateClient() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build()
                .getClient();

        Response r = target("/client")
                .queryParam("client_id", IdUtil.toString(c.getId()))
                .queryParam("client_secret", c.getClientSecret())
                .request()
                .get();

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a public client passes.
     */
    @Test
    public void testValidPublicClient() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.Implicit, false)
                .build()
                .getClient();

        Response r = target("/client")
                .queryParam("client_id", IdUtil.toString(c.getId()))
                .request()
                .get();

        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that PUT fails.
     */
    @Test
    public void testPUTClient() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.Implicit, false)
                .build()
                .getClient();

        Form requestData = new Form();
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .queryParam("client_id", IdUtil.toString(c.getId()))
                .request()
                .put(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that POST fails.
     */
    @Test
    public void tesPOSTClient() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.Implicit, false)
                .build()
                .getClient();

        Form requestData = new Form();
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .queryParam("client_id", IdUtil.toString(c.getId()))
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }
}
