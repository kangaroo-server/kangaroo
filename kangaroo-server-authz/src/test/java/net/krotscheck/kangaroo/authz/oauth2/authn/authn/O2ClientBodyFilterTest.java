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
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for POST-form based authentication of client requests.
 *
 * @author Michael Krotscheck
 */
public final class O2ClientBodyFilterTest extends ContainerTest {

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
     * Assert that a request with no form parameters fails.
     */
    @Test
    public void testNoBody() {
        Form requestData = new Form();
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with no client request parameters fails.
     */
    @Test
    public void testNoClientAuthParams() {
        Form requestData = new Form();
        requestData.param("hello", "world");
        requestData.param("hello", "kitty");
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request made with an invalid body encoding type fails.
     */
    @Test
    public void testInvalidBodyEncodingType() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication().getClient();

        Map<String, String> requestData = new HashMap<>();
        // Valid client.
        requestData.put("client_id", IdUtil.toString(c.getId()));
        requestData.put("client_secret", c.getClientSecret());
        Entity<Map<String, String>> testEntity =
                Entity.entity(requestData, APPLICATION_JSON_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a malformed client id fails.
     */
    @Test
    public void testMalformedClientId() {
        Form requestData = new Form();
        requestData.param("client_id", "malformed client id");
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a nonexistent client id fails.
     */
    @Test
    public void testNonexistentClientId() {
        Form requestData = new Form();
        requestData.param("client_id",
                IdUtil.toString(IdUtil.next()));
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a nonexistent client id fails.
     */
    @Test
    public void testBadSecret() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build()
                .getClient();
        assertTrue(c.isPrivate());

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c.getId()));
        requestData.param("client_secret", "bad_secret");
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with duplicate client id's fails.
     */
    @Test
    public void testDuplicateClientIds() {
        ApplicationBuilder b = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder();
        Client c1 = b.client(ClientType.AuthorizationGrant, true)
                .build().getClient();
        Client c2 = b.client(ClientType.AuthorizationGrant, true)
                .build().getClient();

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c1.getId()));
        requestData.param("client_id", IdUtil.toString(c2.getId()));
        requestData.param("client_secret", c1.getClientSecret());
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with duplicate client secrets fails.
     */
    @Test
    public void testDuplicateClientSecrets() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build()
                .getClient();

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c.getId()));
        requestData.param("client_secret", c.getClientSecret());
        requestData.param("client_secret", c.getClientSecret());
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID of a public client, and no
     * secret fails.
     */
    @Test
    public void testPublicClient() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, false)
                .build()
                .getClient();

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c.getId()));
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID of a public client, on a
     * resource that does not permit public clients, is not permitted.
     */
    @Test
    public void testPublicClientNotPermitted() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, false)
                .build()
                .getClient();

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c.getId()));
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client/private")
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID and secret pass.
     */
    @Test
    public void testPrivateClient() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build()
                .getClient();

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c.getId()));
        requestData.param("client_secret", c.getClientSecret());
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .post(testEntity);

        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID of a private client, on a
     * resource that does not permit private clients, is not permitted.
     */
    @Test
    public void testPrivateClientNotPermitted() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build()
                .getClient();

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c.getId()));
        requestData.param("client_secret", c.getClientSecret());
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client/public")
                .request()
                .post(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID and secret pass.
     */
    @Test
    public void testPUTClient() {
        Client c = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .client(ClientType.AuthorizationGrant, true)
                .build()
                .getClient();

        Form requestData = new Form();
        requestData.param("client_id", IdUtil.toString(c.getId()));
        requestData.param("client_secret", c.getClientSecret());
        Entity<Form> testEntity = Entity.entity(requestData,
                APPLICATION_FORM_URLENCODED_TYPE);

        Response r = target("/client")
                .request()
                .put(testEntity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }
}
