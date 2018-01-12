/*
 * Copyright (c) 2017 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.oauth2.authn;

import net.krotscheck.kangaroo.authz.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.oauth2.authn.authn.O2TestResource;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static net.krotscheck.kangaroo.util.HttpUtil.authHeaderBasic;
import static org.junit.Assert.assertEquals;

/**
 * Assert that binding the Authn feature includes expected components.
 *
 * @author Michael Krotscheck
 */
public final class O2AuthDynamicFeatureTest extends ContainerTest {

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
    public void testFailedAuth() {
        Response r = target("/")
                .request()
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
    }

    /**
     * Assert that a request with a valid ID and secret pass.
     */
    @Test
    public void testValidAuth() {
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
