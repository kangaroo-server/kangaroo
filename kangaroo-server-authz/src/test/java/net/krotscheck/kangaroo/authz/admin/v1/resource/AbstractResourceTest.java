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

package net.krotscheck.kangaroo.authz.admin.v1.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.kangaroo.authz.admin.AdminV1API;
import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.authz.common.authenticator.test.TestAuthenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientRedirect;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientReferrer;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jerseyTest.ContainerTest;
import net.krotscheck.kangaroo.test.HttpUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.hibernate.collection.internal.PersistentSortedMap;
import org.hibernate.internal.SessionImpl;
import org.junit.Assert;
import org.junit.ClassRule;

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
import java.util.SortedMap;
import java.util.TreeMap;
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
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE);

    /**
     * The current running test application.
     */
    private ResourceConfig testApplication;

    /**
     * Create the application under test.
     *
     * @return A configured api servlet.
     */
    @Override
    protected final ResourceConfig createApplication() {
        if (testApplication == null) {
            testApplication = new AdminV1API();
            testApplication.register(new TestAuthenticator.Binder());
        }
        return testApplication;
    }

    /**
     * Return the admin context.
     *
     * @return The admin context.
     */
    public final ApplicationContext getAdminContext() {
        return TEST_DATA_RESOURCE.getAdminApplication();
    }

    /**
     * Return the second application context (not the admin context).
     *
     * @return The secondary context in this test.
     */
    protected final ApplicationContext getSecondaryContext() {
        return TEST_DATA_RESOURCE.getSecondaryApplication();
    }

    /**
     * Return the system configuration.
     *
     * @return The secondary context in this test.
     */
    protected final Configuration getSystemConfig() {
        return TEST_DATA_RESOURCE.getSystemConfiguration();
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
    protected final Response getEntity(final AbstractAuthzEntity entity,
                                       final OAuthToken token) {
        URI location = getUrlForEntity(entity);
        String authHeader = null;

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }

        return getEntity(location, authHeader);
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
        URI location = getUrlForId(id);
        return getEntity(location, authHeader);
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
        String authHeader = null;

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }

        return getEntity(location, authHeader);
    }

    /**
     * Issue a GET request to retrieve the given path with the provided token.
     *
     * @param location   The URL to get.
     * @param authHeader the authorization header to use.
     * @return The received response.
     */
    protected final Response getEntity(final URI location,
                                       final String authHeader) {
        Builder t = target(location.getPath()).request();

        if (!StringUtils.isEmpty(authHeader)) {
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
    protected final Response postEntity(final AbstractAuthzEntity entity,
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
    protected final Response postEntity(final AbstractAuthzEntity entity,
                                        final String authHeader) {
        // Set a default.
        URI location = getUrlForId("");

        // If we have an entity, use it.
        if (entity != null) {
            // Temporarily null any ID from the entity so we generate a proper
            // POST path.
            UUID oldId = entity.getId();
            entity.setId(null);
            location = getUrlForEntity(entity);
            entity.setId(oldId);
        }

        // Issue the request.
        Builder t = target(location.getPath()).request();

        if (!StringUtils.isEmpty(authHeader)) {
            t.header(HttpHeaders.AUTHORIZATION, authHeader);
        }

        Entity<AbstractAuthzEntity> postEntity = Entity.entity(entity,
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
    protected final Response putEntity(final AbstractAuthzEntity entity,
                                       final OAuthToken token) {
        URI location = getUrlForEntity(entity);
        String authHeader = null;

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }
        return putEntity(location, entity, authHeader);
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
                                       final AbstractAuthzEntity entity,
                                       final String authHeader) {
        URI location = getUrlForId(id);
        return putEntity(location, entity, authHeader);
    }

    /**
     * Utility method that simplifies issuing a PUT request with a bearer
     * token.
     *
     * @param location   The URI to post to.
     * @param entity     The Entity to post
     * @param authHeader The full authorization header.
     * @return The received response.
     */
    protected final Response putEntity(final URI location,
                                       final AbstractAuthzEntity entity,
                                       final String authHeader) {
        Builder t = target(location.getPath()).request();

        if (!StringUtils.isEmpty(authHeader)) {
            t.header(HttpHeaders.AUTHORIZATION, authHeader);
        }

        Entity<AbstractAuthzEntity> putEntity = Entity.entity(entity,
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
    protected final Response deleteEntity(final AbstractAuthzEntity entity,
                                          final OAuthToken token) {
        String authHeader = null;

        if (token != null) {
            authHeader = HttpUtil.authHeaderBearer(token.getId());
        }
        URI location = getUrlForEntity(entity);
        return deleteEntity(location, authHeader);
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
        URI location = getUrlForId(appId);
        return deleteEntity(location, authHeader);
    }

    /**
     * Utility method that simplifies issuing a DELETE request with a bearer
     * token.
     *
     * @param location   The URI to post to.
     * @param authHeader The full authorization header.
     * @return The received response.
     */
    protected final Response deleteEntity(final URI location,
                                          final String authHeader) {
        Builder t = target(location.getPath()).request();

        if (!StringUtils.isEmpty(authHeader)) {
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
        WebTarget t = target(getBrowseUrl().getPath());
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
        WebTarget t = target(getSearchUrl().getPath());
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
    protected abstract URI getUrlForId(String id);

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    protected abstract URI getUrlForEntity(AbstractAuthzEntity entity);

    /**
     * Construct the URL for the browse endpoint.
     *
     * @return The resource URL.
     */
    protected final URI getBrowseUrl() {
        return getUrlForId("");
    }

    /**
     * Construct the URL for the search endpoint.
     *
     * @return The resource URL.
     */
    protected final URI getSearchUrl() {
        return getUrlForId("search");
    }

    /**
     * Helper method, determines if a given token has read access, or
     * ownership of, an entity.
     *
     * @param entity The entity to check.
     * @param token  The token with the appropriate credentials.
     * @return True if the entity is owned, otherwise false.
     */
    protected final boolean isAccessible(final AbstractAuthzEntity entity,
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
    protected final boolean isAccessible(final AbstractAuthzEntity entity,
                                         final OAuthToken token,
                                         final String adminScope) {
        Session session = (SessionImpl) ((PersistentSortedMap) token
                .getScopes())
                .getSession();
        session.beginTransaction();
        try {
            if (token.getScopes().containsKey(adminScope)) {
                return true;
            }
            if (token.getIdentity() == null) {
                return false;
            }
            return token.getIdentity().getUser().equals(entity.getOwner());

        } finally {
            session.getTransaction().commit();
        }
    }


    /**
     * Assert that two entities - using reflection - are exactly the same.
     * For entity references, this method will compare ID's. For primitives
     * it will compare content. It will skip createdDate, modifiedDate, and ID.
     *
     * @param left  The first entity.
     * @param right The second entity.
     */
    protected final void assertContentEquals(final AbstractAuthzEntity left,
                                             final AbstractAuthzEntity right) {
        Set<String> omittedFields = new HashSet<>();
        omittedFields.add("id");
        omittedFields.add("createdDate");
        omittedFields.add("modifiedDate");
        omittedFields.add("owner");
        omittedFields.add("password");
        omittedFields.add("redirects");
        omittedFields.add("referrers");

        if (left instanceof ClientRedirect || left instanceof ClientReferrer) {
            omittedFields.add("client");
        }

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

    /**
     * Returns an entity, attached to the test's current session.
     *
     * @param instance The instance to hydrate.
     * @param <T>      The instance type.
     * @return This same database instance, attached to the test's session.
     */
    protected final <T extends AbstractAuthzEntity> T getAttached(final T instance) {
        return (T) getSession().get(instance.getClass(),
                instance.getId());
    }

    /**
     * Returns an entity, attached to the test's current session.
     *
     * @param instances The instances to hydrate.
     * @param <T>       The instance type.
     * @return This same database instance, attached to the test's session.
     */
    protected final <T extends AbstractAuthzEntity> List<T> getAttached(
            final List<T> instances) {
        List<T> attached = new ArrayList<T>();
        for (T instance : instances) {
            attached.add(getAttached(instance));
        }
        return attached;
    }

    /**
     * Returns an entity, attached to the test's current session.
     *
     * @param instances The instances to hydrate.
     * @param <T>       The instance type.
     * @return This same database instance, attached to the test's session.
     */
    protected final <T extends AbstractAuthzEntity> SortedMap<String, T> getAttached(
            final SortedMap<String, T> instances) {
        SortedMap<String, T> attached = new TreeMap<>();
        for (Entry<String, T> entry : instances.entrySet()) {
            attached.put(entry.getKey(), getAttached(entry.getValue()));
        }
        return attached;
    }
}
