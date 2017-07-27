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

package net.krotscheck.kangaroo.authz.common.cors;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.authz.admin.AdminV1API;
import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.test.jerseyTest.ContainerTest;
import net.krotscheck.kangaroo.test.HttpUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Unit tests for CORS specifically used in the authz application.
 *
 * @author Michael Krotscheck
 */
public final class AuthzCORSFeatureTest extends ContainerTest {

    /**
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE);

    /**
     * Create the application under test.
     *
     * @return A configured api servlet.
     */
    @Override
    protected ResourceConfig createApplication() {
        return new AdminV1API();
    }

    /**
     * CORS overall works, we just need to make sure that the feature is
     * bound properly and spits out the list parameters.
     */
    @Test
    public void testListResponse() {
        ApplicationContext context = TEST_DATA_RESOURCE.getAdminApplication();
        context.getBuilder().referrer("http://valid.example.com").build();

        OAuthToken adminAppToken = context
                .getBuilder()
                .bearerToken(Scope.APPLICATION_ADMIN)
                .build()
                .getToken();

        Response r = target("/application/")
                .request()
                .header(HttpHeaders.ORIGIN, "http://valid.example.com")
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(adminAppToken.getId()))
                .get();

        MultivaluedMap<String, Object> headers = r.getHeaders();
        List<Object> exposedHeaders = headers.get(HttpHeaders
                .ACCESS_CONTROL_EXPOSE_HEADERS);

        Assert.assertTrue(exposedHeaders
                .indexOf(ApiParam.LIMIT_HEADER.toLowerCase()) > -1);
        Assert.assertTrue(exposedHeaders
                .indexOf(ApiParam.OFFSET_HEADER.toLowerCase()) > -1);
        Assert.assertTrue(exposedHeaders
                .indexOf(ApiParam.ORDER_HEADER.toLowerCase()) > -1);
        Assert.assertTrue(exposedHeaders
                .indexOf(ApiParam.SORT_HEADER.toLowerCase()) > -1);
        Assert.assertTrue(exposedHeaders
                .indexOf(ApiParam.TOTAL_HEADER.toLowerCase()) > -1);
    }
}
