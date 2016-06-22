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
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
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
public final class UserServiceTest extends ContainerTest {

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
        // Load data
        loadTestData(TEST_DATA);
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
        Assert.assertEquals("00000000-0000-0000-0000-000000000004",
                users.get(0).getId().toString());
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
        Assert.assertEquals("00000000-0000-0000-0000-000000000003",
                users.get(0).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000004",
                users.get(1).getId().toString());
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
                .queryParam("sort", "createdDate")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals("00000000-0000-0000-0000-000000000004",
                users.get(0).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000003",
                users.get(1).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000002",
                users.get(2).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000001",
                users.get(3).getId().toString());
    }

    /**
     * Assert that an invalid sort field gives us an expected error.
     */
    @Test
    public void testBrowseUsersSortInvalid() {
        Response response = target("/user")
                .queryParam("sort", "invalidfield")
                .request()
                .get();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }

    /**
     * Assert that you can browse users and set ordering asc.
     */
    @Test
    public void testBrowseUsersSortOrderAsc() {
        Response response = target("/user")
                .queryParam("sort", "createdDate")
                .queryParam("order", "asc")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals("00000000-0000-0000-0000-000000000004",
                users.get(0).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000003",
                users.get(1).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000002",
                users.get(2).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000001",
                users.get(3).getId().toString());
    }

    /**
     * Assert that you can browse users and set ordering desc.
     */
    @Test
    public void testBrowseUsersSortOrderDesc() {
        Response response = target("/user")
                .queryParam("sort", "createdDate")
                .queryParam("order", "desc")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals("00000000-0000-0000-0000-000000000001",
                users.get(0).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000002",
                users.get(1).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000003",
                users.get(2).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000004",
                users.get(3).getId().toString());
    }

    /**
     * Assert that an invalid order maps to "ASC".
     */
    @Test
    public void testBrowseUsersSortOrderInvalid() {
        Response response = target("/user")
                .queryParam("sort", "createdDate")
                .queryParam("order", "aasdfasdf")
                .request()
                .get();
        List<User> users = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, users.size());

        Assert.assertEquals("00000000-0000-0000-0000-000000000004",
                users.get(0).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000003",
                users.get(1).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000002",
                users.get(2).getId().toString());
        Assert.assertEquals("00000000-0000-0000-0000-000000000001",
                users.get(3).getId().toString());
    }
}
