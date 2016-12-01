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

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Strings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.servlet.admin.v1.AdminV1API;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.test.ContainerTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.HttpUtil;
import net.krotscheck.kangaroo.test.TestAuthenticator;
import org.apache.commons.configuration.Configuration;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Assert;

import javax.persistence.Transient;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Abstract test harness for the administration API. Handles all of our data
 * bootstrapping.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractResourceTest extends ContainerTest {

    /**
     * The Admin context.
     */
    private EnvironmentBuilder admin;

    /**
     * DB Configuration, for ease of access.
     */
    private Configuration systemConfig;

    /**
     * Create the application under test.
     *
     * @return A configured api servlet.
     */
    @Override
    protected final ResourceConfig createApplication() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        ResourceConfig v1Api = new AdminV1API();
        v1Api.register(new TestAuthenticator.Binder());
        return v1Api;
    }

    /**
     * Create an admin fixture, then use that to feed any other data
     * bootstrapping.
     *
     * @param session The session to use to build the environment.
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception An exception that indicates a failed fixture load.
     */
    @Override
    public final List<EnvironmentBuilder> fixtures(final Session session)
            throws Exception {

        SessionFactory f = getSessionFactory();

        ServletConfigFactory factory = new ServletConfigFactory(f);
        systemConfig = factory.provide();

        String configId = systemConfig.getString(Config.APPLICATION_ID);
        UUID appId = UUID.fromString(configId);
        Application a = session.get(Application.class, appId);

        admin = new EnvironmentBuilder(session, a);

        // Build the fixtures list from this and the implementing class.
        List<EnvironmentBuilder> fixtures = new ArrayList<>();
        fixtures.add(admin);
        fixtures.addAll(fixtures(session, admin));
        return fixtures;
    }

    /**
     * After everything has been cleared, we need to delete the configured
     * admin application reference.
     */
    @After
    public final void resetAdminApplication() {
        // Manually delete the admin application, so it can be recreated in a
        // clean state.
        Session s = getSession();

        // Clear out any ownership references, so we don't have any cyclic
        // dependencies.
        Query removeOwners =
                s.createQuery("update Application set owner = null");
        removeOwners.executeUpdate();

        // Delete all the applications. This should cascade to delete all
        // other database records.
        Query remove = s.createQuery("delete Application");
        remove.executeUpdate();

        systemConfig.clear();
    }

    /**
     * Provided the admin context, build a list of all additional
     * applications required for this test.
     *
     * @param session      The hibernate session.
     * @param adminContext The admin context
     * @return A list of fixtures.
     * @throws Exception Thrown if something untoward happens.
     */
    public abstract List<EnvironmentBuilder> fixtures(
            Session session,
            EnvironmentBuilder adminContext)
            throws Exception;

    /**
     * Return the admin context.
     *
     * @return The admin context.
     */
    public final EnvironmentBuilder getAdminContext() {
        return admin;
    }

    /**
     * Return the convenience system configuration.
     *
     * @return The system configuration.
     */
    public final Configuration getSystemConfig() {
        return systemConfig;
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    protected abstract String getAdminScope();

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    protected abstract String getRegularScope();


    /**
     * Utility method that simplifies issuing a GET request with a bearer
     * token.
     *
     * @param entity The entity to get.
     * @param token  The bearer token.
     * @return The received response.
     */
    protected final Response getEntity(final AbstractEntity entity,
                                       final OAuthToken token) {
        String id = null;
        String authHeader = null;

        if (entity != null && entity.getId() != null) {
            id = entity.getId().toString();
        }

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }

        return getEntity(id, authHeader);
    }

    /**
     * Issue a GET request to retrieve the given ID with the provided token.
     * The URL will be constructed from the abstract 'getUrlForId' method.
     *
     * @param id         The ID to GET
     * @param authHeader the authorization header to use.
     * @return The received response.
     */
    protected final Response getEntity(final String id,
                                       final String authHeader) {
        String url = getUrlForId(id);
        Builder t = target(url).request();

        if (!Strings.isNullOrEmpty(authHeader)) {
            t.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return t.get();
    }

    /**
     * Issue a GET request to retrieve the given path with the provided token.
     *
     * @param location The URL to get.
     * @param token    The oauth token to use.
     * @return The received response.
     */
    protected final Response getEntity(final URI location,
                                       final OAuthToken token) {
        Builder t = target(location.getPath()).request();

        if (token != null) {
            String authHeader =
                    HttpUtil.authHeaderBearer(token.getId());
            t.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return t.get();
    }

    /**
     * Utility method that simplifies issuing a POST request with a bearer
     * token.
     *
     * @param entity The entity to post
     * @param token  The OAuth Token.
     * @return The received response.
     */
    protected final Response postEntity(final AbstractEntity entity,
                                        final OAuthToken token) {
        String authHeader = null;

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }
        return postEntity(entity, authHeader);
    }

    /**
     * Utility method that simplifies issuing a POST request with a bearer
     * token.
     *
     * @param entity     The entity to post
     * @param authHeader the authorization header to use.
     * @return The received response.
     */
    protected final Response postEntity(final AbstractEntity entity,
                                        final String authHeader) {
        String url = getUrlForId(null);
        Builder t = target(url).request();

        if (!Strings.isNullOrEmpty(authHeader)) {
            t.header(HttpHeaders.AUTHORIZATION, authHeader);
        }

        Entity<AbstractEntity> postEntity = Entity.entity(entity,
                MediaType.APPLICATION_JSON_TYPE);
        return t.post(postEntity);
    }

    /**
     * Utility method that simplifies issuing a PUT request with a bearer
     * token.
     *
     * @param entity The entity to post
     * @param token  The token to use.
     * @return The received response.
     */
    protected final Response putEntity(final AbstractEntity entity,
                                       final OAuthToken token) {
        String authHeader = null;

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }
        return putEntity(entity.getId().toString(), entity, authHeader);
    }

    /**
     * Utility method that simplifies issuing a PUT request with a bearer
     * token.
     *
     * @param id         The ID to post to.
     * @param entity     The Entity to post
     * @param authHeader The full authorization header.
     * @return The received response.
     */
    protected final Response putEntity(final String id,
                                       final AbstractEntity entity,
                                       final String authHeader) {
        String url = getUrlForId(id);
        Builder t = target(url).request();

        if (!Strings.isNullOrEmpty(authHeader)) {
            t.header(HttpHeaders.AUTHORIZATION, authHeader);
        }

        Entity<AbstractEntity> putEntity = Entity.entity(entity,
                MediaType.APPLICATION_JSON_TYPE);
        return t.put(putEntity);
    }

    /**
     * Utility method that simplifies issuing a DELETE request with a bearer
     * token.
     *
     * @param entity The entity to delete
     * @param token  The auth token to use.
     * @return The received response.
     */
    protected final Response deleteEntity(final AbstractEntity entity,
                                          final OAuthToken token) {
        String authHeader = null;

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }
        return deleteEntity(entity.getId().toString(), authHeader);
    }

    /**
     * Utility method that simplifies issuing a DELETE request with a bearer
     * token.
     *
     * @param appId      The Application id to delete
     * @param authHeader The full authorization header.
     * @return The received response.
     */
    protected final Response deleteEntity(final String appId,
                                          final String authHeader) {
        String url = getUrlForId(appId);
        Builder t = target(url).request();

        if (!Strings.isNullOrEmpty(authHeader)) {
            t.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return t.delete();
    }

    /**
     * Execute a browse request.
     *
     * @param params The query parameters.
     * @param token  The auth token.
     * @return The received response.
     */
    protected final Response browse(final Map<String, String> params,
                                    final OAuthToken token) {
        WebTarget t = target(getUrlForId(null));
        for (Entry<String, String> e : params.entrySet()) {
            t = t.queryParam(e.getKey(), e.getValue());
        }
        Builder b = t.request();
        if (token != null) {
            b = b.header(HttpHeaders.AUTHORIZATION,
                    HttpUtil.authHeaderBearer(token.getId()));
        }
        return b.get();
    }

    /**
     * Execute a search request.
     *
     * @param params The query parameters.
     * @param token  The auth token.
     * @return The received response.
     */
    protected final Response search(final Map<String, String> params,
                                    final OAuthToken token) {
        WebTarget t = target(getUrlForId("search"));
        for (Entry<String, String> e : params.entrySet()) {
            t = t.queryParam(e.getKey(), e.getValue());
        }
        Builder b = t.request();
        if (token != null) {
            b = b.header(HttpHeaders.AUTHORIZATION,
                    HttpUtil.authHeaderBearer(token.getId()));
        }
        return b.get();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    protected abstract String getUrlForId(String id);

    /**
     * Helper method, determines if a given token has read access, or
     * ownership of, an entity.
     *
     * @param entity The entity to check.
     * @param token  The token with the appropriate credentials.
     * @return True if the entity is owned, otherwise false.
     */
    protected final boolean isAccessible(final AbstractEntity entity,
                                         final OAuthToken token) {
        return isAccessible(entity, token, getAdminScope());
    }

    /**
     * Helper method, determines if a given token has read access, or
     * ownership of, an entity.
     *
     * @param entity     The entity to check.
     * @param token      The token with the appropriate credentials.
     * @param adminScope The required admin scope.
     * @return True if the entity is owned, otherwise false.
     */
    protected final boolean isAccessible(final AbstractEntity entity,
                                         final OAuthToken token,
                                         final String adminScope) {
        if (token.getScopes().containsKey(adminScope)) {
            return true;
        }
        if (token.getIdentity() == null) {
            return false;
        }
        return token.getIdentity().getUser().equals(entity.getOwner());
    }

    /**
     * Assert that two entities - using reflection - are exactly the same.
     * For entity references, this method will compare ID's. For primitives
     * it will compare content. It will skip createdDate, modifiedDate, and ID.
     *
     * @param left  The first entity.
     * @param right The second entity.
     */
    protected final void assertContentEquals(final AbstractEntity left,
                                             final AbstractEntity right) {
        Set<String> omittedFields = new HashSet<>();
        omittedFields.add("id");
        omittedFields.add("createdDate");
        omittedFields.add("modifiedDate");
        omittedFields.add("owner");
        omittedFields.add("password");

        // First assert that we have the same type.
        Assert.assertEquals(left.getClass(), right.getClass());

        try {
            PropertyDescriptor[] descriptors =
                    Introspector.getBeanInfo(left.getClass())
                            .getPropertyDescriptors();

            for (PropertyDescriptor descriptor : descriptors) {
                // Skip ID and created/modifiedDate
                if (omittedFields.contains(descriptor.getName())) {
                    continue;
                }

                Method readMethod = descriptor.getReadMethod();
                // Filter out anything that's transient, as that's usually a
                // computed property.
                List<Annotation> annotations = Arrays.asList(
                        readMethod.getDeclaredAnnotations())
                        .stream()
                        .filter(a -> a.annotationType().equals(Transient.class)
                                || a.annotationType().equals(JsonIgnore.class))
                        .collect(Collectors.toList());
                if (annotations.size() > 0) {
                    continue;
                }


                Object leftValue = readMethod.invoke(left);
                Object rightValue = readMethod.invoke(right);

                Assert.assertEquals(leftValue, rightValue);
            }

        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * Test for a specific error response.
     *
     * @param r                  The error response.
     * @param expectedHttpStatus The expected http status.
     */
    protected final void assertErrorResponse(final Response r,
                                             final Status expectedHttpStatus) {
        String expectedMessage = expectedHttpStatus.getReasonPhrase()
                .toLowerCase().replace(" ", "_");
        assertErrorResponse(r, expectedHttpStatus, expectedMessage);
    }

    /**
     * Test for a specific error response.
     *
     * @param r                  The error response.
     * @param expectedHttpStatus The expected http status.
     * @param message            The expected message.
     */
    protected final void assertErrorResponse(final Response r,
                                             final Status expectedHttpStatus,
                                             final String message) {
        assertErrorResponse(r, expectedHttpStatus.getStatusCode(), message);
    }

    /**
     * Test for a specific error response, code, and message.
     *
     * @param r               THe response to test.
     * @param statusCode      The expected status code.
     * @param expectedMessage The expected message.
     */
    protected final void assertErrorResponse(final Response r,
                                             final int statusCode,
                                             final String expectedMessage) {
        Assert.assertFalse(
                String.format("%s must not be a success code", r.getStatus()),
                r.getStatus() < 400);
        ErrorResponse response = r.readEntity(ErrorResponse.class);
        Assert.assertEquals(statusCode, r.getStatus());
        Assert.assertEquals(expectedMessage, response.getError());
    }
}
