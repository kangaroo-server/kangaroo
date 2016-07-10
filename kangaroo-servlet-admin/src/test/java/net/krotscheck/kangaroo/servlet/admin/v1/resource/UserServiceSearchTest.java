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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.IFixture;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Tests for the UserService API.
 *
 * @author Michael Krotscheck
 */
public final class UserServiceSearchTest extends AbstractResourceTest {

    /**
     * List of created users, for test comparisons.
     */
    private List<User> users = new ArrayList<>();

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
                .claim("name", "B Test User Search Search")
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
        List<User> results = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("1", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals("Single", response.getHeaderString("Query"));
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(users.get(3).getId(), results.get(0).getId());
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
        List<User> results = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("2", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals("Search", response.getHeaderString("Query"));
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(users.get(2).getId(), results.get(0).getId());
        Assert.assertEquals(users.get(3).getId(), results.get(1).getId());
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
        List<User> results = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, results.size());

        Assert.assertEquals(users.get(0).getId(), results.get(0).getId());
        Assert.assertEquals(users.get(1).getId(), results.get(1).getId());
        Assert.assertEquals(users.get(2).getId(), results.get(2).getId());
        Assert.assertEquals(users.get(3).getId(), results.get(3).getId());
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
        List<User> results = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, results.size());

        Assert.assertEquals(users.get(0).getId(), results.get(0).getId());
        Assert.assertEquals(users.get(1).getId(), results.get(1).getId());
        Assert.assertEquals(users.get(2).getId(), results.get(2).getId());
        Assert.assertEquals(users.get(3).getId(), results.get(3).getId());
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
        List<User> results = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, results.size());

        Assert.assertEquals(users.get(3).getId(), results.get(0).getId());
        Assert.assertEquals(users.get(2).getId(), results.get(1).getId());
        Assert.assertEquals(users.get(1).getId(), results.get(2).getId());
        Assert.assertEquals(users.get(0).getId(), results.get(3).getId());
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
        List<User> results = response.readEntity(new GenericType<List<User>>() {

        });

        Assert.assertEquals("4", response.getHeaderString("Total"));
        Assert.assertEquals("0", response.getHeaderString("Offset"));
        Assert.assertEquals("10", response.getHeaderString("Limit"));
        Assert.assertEquals(4, results.size());

        Assert.assertEquals(users.get(0).getId(), results.get(0).getId());
        Assert.assertEquals(users.get(1).getId(), results.get(1).getId());
        Assert.assertEquals(users.get(2).getId(), results.get(2).getId());
        Assert.assertEquals(users.get(3).getId(), results.get(3).getId());
    }
}
