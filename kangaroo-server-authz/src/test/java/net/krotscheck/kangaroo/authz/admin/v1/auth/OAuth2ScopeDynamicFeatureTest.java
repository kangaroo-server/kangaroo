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

package net.krotscheck.kangaroo.authz.admin.v1.auth;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Unit tests for the oauth2 feature.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2ScopeDynamicFeatureTest extends ContainerTest {

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
        a.register(DatabaseFeature.class);
        a.register(OAuth2ScopeDynamicFeature.class);
        a.register(MockService.class);
        a.register(SecondMockService.class);
        a.register(ThirdMockService.class);
        a.register(ExceptionFeature.class);
        a.register(ConfigurationFeature.class);
        a.register(new ServletConfigFactory.Binder());
        a.register(new FirstRunContainerLifecycleListener.Binder());
        return a;
    }

    /**
     * Test the denyall flag.
     */
    @Test
    public void testDenyAllMethod() {
        Response response = target("/first/deny")
                .request()
                .get();
        Assert.assertEquals(401, response.getStatus());
    }

    /**
     * Test the permitall flag.
     */
    @Test
    public void testPermitAllMethod() {
        Response response = target("/first/permit")
                .request()
                .get();
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test the Scopes flag.
     */
    @Test
    public void testValidScopesMethod() {
        OAuthToken token = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .bearerToken(Scope.CLIENT)
                .build()
                .getToken();

        Response response = target("/first/scopes")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .get();
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test the Scopes flag.
     */
    @Test
    public void testInvalidScopesMethod() {
        OAuthToken token = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .bearerToken(Scope.USER)
                .build()
                .getToken();

        Response response = target("/first/scopes")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .get();
        Assert.assertEquals(403, response.getStatus());
    }

    /**
     * Test the Scopes flag.
     */
    @Test
    public void testValidScopesClass() {
        OAuthToken token = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .bearerToken(Scope.CLIENT)
                .build()
                .getToken();

        Response response = target("/second")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .get();
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test the Scopes flag.
     */
    @Test
    public void testInvalidScopeClass() {
        OAuthToken token = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .bearerToken(Scope.USER)
                .build()
                .getToken();

        Response response = target("/second")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .get();
        Assert.assertEquals(403, response.getStatus());
    }

    /**
     * Test class-based permit all.
     */
    @Test
    public void testPermitAllClass() {
        OAuthToken token = TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .bearerToken(Scope.USER)
                .build()
                .getToken();

        Response response = target("/third")
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .get();
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * A simple endpoint that contains annotated methods.
     *
     * @author Michael Krotscheck
     */
    @Path("/first")
    public static final class MockService {

        /**
         * Return true.
         *
         * @return HTTP OK response
         */
        @GET
        @Path("/deny")
        @DenyAll
        @Produces(MediaType.APPLICATION_JSON)
        public Response denyAll() {
            return Response.status(Status.OK).build();
        }

        /**
         * Return true.
         *
         * @return HTTP OK response
         */
        @GET
        @Path("/permit")
        @PermitAll
        @Produces(MediaType.APPLICATION_JSON)
        public Response permitAll() {
            return Response.status(Status.OK).build();
        }

        /**
         * Return true.
         *
         * @return HTTP OK response
         */
        @GET
        @Path("/scopes")
        @ScopesAllowed(Scope.CLIENT)
        @Produces(MediaType.APPLICATION_JSON)
        public Response permitScopes() {
            return Response.status(Status.OK).build();
        }
    }

    /**
     * A simple endpoint that requires scopes.
     *
     * @author Michael Krotscheck
     */
    @Path("/second")
    @ScopesAllowed(Scope.CLIENT)
    public static final class SecondMockService {

        /**
         * Return true.
         *
         * @return HTTP OK response
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response permitScopes() {
            return Response.status(Status.OK).build();
        }
    }

    /**
     * A simple endpoint that requires nothing.
     *
     * @author Michael Krotscheck
     */
    @Path("/third")
    @PermitAll
    public static final class ThirdMockService {

        /**
         * Return true.
         *
         * @return HTTP OK response
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response permitAll() {
            return Response.status(Status.OK).build();
        }
    }
}
