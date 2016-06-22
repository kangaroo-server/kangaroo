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

package net.krotscheck.api.admin.v1.user.resource;

import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.features.database.DatabaseFeature;
import net.krotscheck.features.database.entity.User;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.test.ContainerTest;
import net.krotscheck.util.ResourceUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

/**
 * Tests for the UserService CRUD actions.
 *
 * @author Michael Krotscheck
 */
public final class UserServiceCRUDTest extends ContainerTest {

    /**
     * The tests's backing data.
     */
    private static final File TEST_DATA = ResourceUtil.getFileForResource(
            "net/krotscheck/api/admin/v1/user/UserServiceTest.xml");

    /**
     * Build and configure the application.
     *
     * @return A configured application.
     */
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        ResourceConfig a = new ResourceConfig();
        a.register(ConfigurationFeature.class);
        a.register(ExceptionFeature.class);
        a.register(JacksonFeature.class);
        a.register(DatabaseFeature.class);
        a.register(UserService.class);

        return a;
    }

    /**
     * Setup Test Data.
     */
    @BeforeClass
    public static void setupTestData() {
        loadTestData(TEST_DATA);
    }

    /**
     * Assert that you can read individual users.
     */
    @Test
    public void testGetUser() {
        User responseUser = target("/user/00000000-0000-0000-0000-000000000001")
                .request()
                .get(User.class);

        Assert.assertEquals("00000000-0000-0000-0000-000000000001",
                responseUser.getId().toString());
    }

    /**
     * Make sure that an unknown user returns a 404.
     */
    @Test
    public void testGetUnknownUser() {
        Response response = target("/user/00000000-0000-0000-0000-000000000100")
                .request()
                .get();

        Assert.assertEquals(404, response.getStatus());
    }

    /**
     * Make sure that an unknown user returns a 404.
     */
    @Test
    public void testGetInvalidUUID() {
        Response response = target("/user/00000000")
                .request()
                .get();

        Assert.assertEquals(404, response.getStatus());
    }

    /**
     * Make sure that an unknown request returns a 404.
     */
    @Test
    public void testGetImproperlyFormattedUser() {
        Response response = target("/user/alskdfjasdf")
                .request()
                .get();

        Assert.assertEquals(404, response.getStatus());
    }
}
