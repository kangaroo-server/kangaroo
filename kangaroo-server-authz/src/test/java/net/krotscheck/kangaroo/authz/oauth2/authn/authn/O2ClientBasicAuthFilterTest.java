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
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.math.BigInteger;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static net.krotscheck.kangaroo.util.HttpUtil.authHeaderBasic;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Basic- based authorization filters.
 *
 * @author Michael Krotscheck
 */
public class O2ClientBasicAuthFilterTest extends ContainerTest {

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
     * Assert that a request with no authorization header fails.
     */
    @Test
    public void testNoAuthorization() {
        Response r = target("/")
                .request()
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with an invalid header fails.
     */
    @Test
    public void testInvalidAuthorizationHeader() {
        Response r = target("/")
                .request()
                .header(AUTHORIZATION, "Not A Valid header")
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a malformed basic header fails.
     */
    @Test
    public void testInvalidBasicHeader() {
        Response r = target("/")
                .request()
                .header(AUTHORIZATION, "Basic some_secluded_rendezvous")
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a malformed user/pass indicates a bad request.
     */
    @Test
    public void testMalformedCredentialsHeader() {
        String header =
                authHeaderBasic("malformed_id", "malformed_pass");

        Response r = target("/")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with nonexistent credentials fails.
     */
    @Test
    public void testNonexistentCredentialsHeader() {
        BigInteger id = IdUtil.next();
        BigInteger pass = IdUtil.next();

        String header = authHeaderBasic(id, IdUtil.toString(pass));

        Response r = target("/")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID of a private client, but invalid
     * secret fails.
     */
    @Test
    public void testBadPasswordHeader() {
        ApplicationContext context =
                TEST_DATA_RESOURCE.getSecondaryApplication().getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .build();
        Client c = context.getClient();

        String header = authHeaderBasic(c.getId(), "invalid_pass");

        Response r = target("/")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID of a public client, and no
     * secret fails.
     */
    @Test
    public void testPublicClientHeader() {
        ApplicationContext context =
                TEST_DATA_RESOURCE.getSecondaryApplication().getBuilder()
                        .client(ClientType.AuthorizationGrant, false)
                        .build();
        Client c = context.getClient();

        String header = authHeaderBasic(c.getId(), "");

        Response r = target("/")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID and secret pass.
     */
    @Test
    public void testValidRequestHeader() {
        ApplicationContext context =
                TEST_DATA_RESOURCE.getSecondaryApplication().getBuilder()
                        .client(ClientType.AuthorizationGrant, true)
                        .build();
        Client c = context.getClient();

        String header = authHeaderBasic(c.getId(), c.getClientSecret());

        Response r = target("/")
                .request()
                .header(AUTHORIZATION, header)
                .get();

        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }
}
