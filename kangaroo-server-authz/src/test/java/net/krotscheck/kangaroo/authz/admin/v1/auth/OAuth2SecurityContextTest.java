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

import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Unit tests for the Security Context.
 *
 * @author Michael Krotscheck
 */
public class OAuth2SecurityContextTest extends DatabaseTest {

    /**
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE);

    /**
     * Test constructor passthrough.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getUserPrincipal() throws Exception {
        ApplicationContext context = TEST_DATA_RESOURCE.getAdminApplication();
        OAuthToken token = context.getBuilder()
                .scope("principal-1")
                .scope("principal-2")
                .bearerToken("principal-1", "principal-2")
                .build()
                .getToken();

        OAuth2SecurityContext securityContext = new OAuth2SecurityContext(
                token, false);

        Assert.assertEquals(false,
                securityContext.isSecure());
        Assert.assertEquals("OAuth2",
                securityContext.getAuthenticationScheme());
        Assert.assertSame(token, securityContext.getUserPrincipal());
        Assert.assertTrue(securityContext.isUserInRole("principal-1"));
        Assert.assertTrue(securityContext.isUserInRole("principal-2"));
        Assert.assertFalse(securityContext.isUserInRole("principal-3"));
    }
}
