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
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.IFixture;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Tests for the UserService CRUD actions.
 *
 * @author Michael Krotscheck
 */
public final class UserServiceCRUDTest extends AbstractResourceTest {

    /**
     * List of created users, for test comparisons.
     */
    private List<User> users = new ArrayList<>();

    /**
     * Authorization header, so we can auth against this resource.
     */
    private String authHeader;

    /**
     * Authorization header with an inappropriate scope.
     */
    private String authHeaderNoScope;

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
                        .scope(Scope.USER)
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

        // Build an auth header.
        context.bearerToken(Scope.USER);
        authHeader = String.format("Bearer %s", context.getToken().getId());

        // Build an auth header with no valid scope.
        context.bearerToken();
        authHeaderNoScope =
                String.format("Bearer %s", context.getToken().getId());

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
                .header(HttpHeaders.AUTHORIZATION, authHeader)
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
                .header(HttpHeaders.AUTHORIZATION, authHeader)
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
                .header(HttpHeaders.AUTHORIZATION, authHeader)
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
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get();

        Assert.assertEquals(404, response.getStatus());
    }

    /**
     * Assert that the resource may only be accessed with a correctly scoped
     * token.
     */
    @Test
    public void testInvalidScopeToken() {
        String path = String.format("/user/%s", users.get(0).getId());
        Response response = target(path)
                .request()
                .header(HttpHeaders.AUTHORIZATION, authHeaderNoScope)
                .get();

        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
}
