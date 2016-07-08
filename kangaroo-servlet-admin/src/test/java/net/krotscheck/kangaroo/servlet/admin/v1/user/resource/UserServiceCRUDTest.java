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

package net.krotscheck.kangaroo.servlet.admin.v1.user.resource;

import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.database.DatabaseFeature;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.IFixture;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

/**
 * Tests for the UserService CRUD actions.
 *
 * @author Michael Krotscheck
 */
public final class UserServiceCRUDTest extends ContainerTest {

    /**
     * List of created users, for test comparisons.
     */
    private List<User> users = new ArrayList<>();

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
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() throws Exception {
        users.clear();
        EnvironmentBuilder context =
                new EnvironmentBuilder(getSession(), "Test Name")
                        .role("owner")
                        .client(ClientType.Implicit, "Test Client")
                        .authenticator("test");

        // User 1
        context.user()
                .identity("remote_id_1")
                .claim("name", "D Test User 1")
                .claim("email", "noreply_1@example.com");
        users.add(context.getUser());

        // User 2
        context.user()
                .identity("remote_id_2")
                .claim("name", "C Test User 2")
                .claim("email", "noreply_2@example.com");
        users.add(context.getUser());

        // User 3
        context.user()
                .identity("remote_id_3")
                .claim("name", "B Test User Search")
                .claim("email", "noreply_3@example.com");
        users.add(context.getUser());

        // User 4
        context.user()
                .identity("remote_id_4")
                .claim("name", "A Test User Single Search")
                .claim("email", "noreply_4@example.com");
        users.add(context.getUser());

        List<IFixture> fixtures = new ArrayList<>();
        fixtures.add(context);
        return fixtures;
    }

    /**
     * Assert that you can read individual users.
     */
    @Test
    public void testGetUser() {
        String path = String.format("/user/%s", users.get(0).getId());
        User responseUser = target(path)
                .request()
                .get(User.class);

        Assert.assertEquals(users.get(0).getId(), responseUser.getId());
    }

    /**
     * Make sure that an unknown user returns a 404.
     */
    @Test
    public void testGetUnknownUser() {
        String path = String.format("/user/%s", UUID.randomUUID());
        Response response = target(path)
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
