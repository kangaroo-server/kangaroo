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

package net.krotscheck.features.user.resource;

import net.krotscheck.features.config.ConfigurationFeature;
import net.krotscheck.features.database.DatabaseFeature;
import net.krotscheck.features.database.entity.User;
import net.krotscheck.features.exception.ExceptionFeature;
import net.krotscheck.features.jackson.JacksonFeature;
import net.krotscheck.test.DatabaseTest;
import net.krotscheck.test.DatabaseUtil;
import net.krotscheck.util.ResourceUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Tests for the UserService API.
 *
 * @author Michael Krotscheck
 */
public final class UserServiceTest extends DatabaseTest {

    /**
     * The tests's backing data.
     */
    private static final File TEST_DATA = ResourceUtil.getFileForResource(
            "net/krotscheck/features/user/UserServiceTest.xml");

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
        DatabaseUtil.loadTestData(TEST_DATA);
    }

    /**
     * Clean test data.
     */
    @AfterClass
    public static void cleanTestData() {
        DatabaseUtil.clearTestData(TEST_DATA);
    }

    /**
     * Test that searching with a stopword returns an error.
     *
     * @throws Exception Unexpected exception.
     */
    @Test
    public void testStopWordSearch() throws Exception {
        Response response = target("/user/search")
                .queryParam("q", "with")
                .request()
                .get();

        Assert.assertEquals(400, response.getStatus());
    }

    /**
     * Test that a single result can be searched for.
     *
     * @throws Exception Unexpected exception.
     */
    @Test
    public void testSingleResultSearch() throws Exception {
        Response response = target("/user/search")
                .queryParam("q", "Single")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("1", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals("Single", response.getHeaderString("Query"));
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(Long.valueOf(4), users.get(0).getId());
    }

    /**
     * Test that we can offset multiple things.
     */
    @Test
    public void testMultiSearchOffset() {
        Response response = target("/user/search")
                .queryParam("q", "User")
                .queryParam("offset", "2")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("2", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals("User", response.getHeaderString("Query"));
        Assert.assertEquals(2, users.size());
    }

    /**
     * Test that we can limit multiple things.
     */
    @Test
    public void testMultiSearchLimit() {
        Response response = target("/user/search")
                .queryParam("q", "User")
                .queryParam("limit", "2")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("2", response.getHeaderString("Limit"));
        Assert.assertEquals("User", response.getHeaderString("Query"));
        Assert.assertEquals(2, users.size());
    }

    /**
     * Test that we can search for multiple things.
     */
    @Test
    public void testMultiResultSearch() {
        Response response = target("/user/search")
                .queryParam("q", "Search")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("2", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals("Search", response.getHeaderString("Query"));
        Assert.assertEquals(2, users.size());
        Assert.assertEquals(Long.valueOf(3), users.get(0).getId());
        Assert.assertEquals(Long.valueOf(4), users.get(1).getId());
    }

    /**
     * Test that bad search results are empty.
     */
    @Test
    public void testEmptyResultSearch() {
        Response response = target("/user/search")
                .queryParam("q", "FooBar")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("0", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals("FooBar", response.getHeaderString("Query"));
        Assert.assertEquals(0, users.size());
    }

    /**
     * Assert that you can browse users.
     */
    @Test
    public void testBrowseUsers() {
        Response response = target("/user")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());
    }

    /**
     * Assert that you can browse users and set limit.
     */
    @Test
    public void testBrowseUsersLimit() {
        Response response = target("/user")
                .queryParam("limit", 2)
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("2", response.getHeaderString("Limit"));
        Assert.assertEquals(2, users.size());
    }

    /**
     * Assert that you can browse users and set offset.
     */
    @Test
    public void testBrowseUsersOffset() {
        Response response = target("/user")
                .queryParam("offset", 2)
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("2", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(2, users.size());
    }

    /**
     * Assert that you can browse users and set ordering.
     */
    @Test
    public void testBrowseUsersSort() {
        Response response = target("/user")
                .queryParam("sort", "name")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals(Long.valueOf(4), users.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), users.get(1).getId());
        Assert.assertEquals(Long.valueOf(2), users.get(2).getId());
        Assert.assertEquals(Long.valueOf(1), users.get(3).getId());
    }

    /**
     * Assert that you can browse users and set ordering asc.
     */
    @Test
    public void testBrowseUsersSortOrderAsc() {
        Response response = target("/user")
                .queryParam("sort", "name")
                .queryParam("order", "asc")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals(Long.valueOf(4), users.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), users.get(1).getId());
        Assert.assertEquals(Long.valueOf(2), users.get(2).getId());
        Assert.assertEquals(Long.valueOf(1), users.get(3).getId());
    }

    /**
     * Assert that you can browse users and set ordering desc.
     */
    @Test
    public void testBrowseUsersSortOrderDesc() {
        Response response = target("/user")
                .queryParam("sort", "name")
                .queryParam("order", "desc")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals(Long.valueOf(1), users.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), users.get(1).getId());
        Assert.assertEquals(Long.valueOf(3), users.get(2).getId());
        Assert.assertEquals(Long.valueOf(4), users.get(3).getId());
    }

    /**
     * Assert that you can browse users and set ordering invalid->asc.
     */
    @Test
    public void testBrowseUsersSortOrderInvalid() {
        Response response = target("/user")
                .queryParam("sort", "name")
                .queryParam("order", "aasdfasdf")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals(Long.valueOf(4), users.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), users.get(1).getId());
        Assert.assertEquals(Long.valueOf(2), users.get(2).getId());
        Assert.assertEquals(Long.valueOf(1), users.get(3).getId());
    }

    /**
     * Assert that you can read individual users.
     */
    @Test
    public void testGetUser() {
        User responseUser = target("/user/1")
                .request()
                .get(User.class);

        Assert.assertEquals(Long.valueOf(1), responseUser.getId());
        Assert.assertEquals("D Test User 1", responseUser.getName());
    }

    /**
     * Make sure that an unknown user returns a 404.
     */
    @Test
    public void testGetUnknownUser() {
        Response response = target("/user/100")
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
