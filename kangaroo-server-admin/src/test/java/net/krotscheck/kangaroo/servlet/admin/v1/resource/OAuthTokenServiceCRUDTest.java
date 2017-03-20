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
 *
 */

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.ApplicationBuilder.ApplicationContext;
import org.hibernate.Session;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test the CRUD methods of the OAuth Token service.
 *
 * @author Michael Krotscheck
 */
public final class OAuthTokenServiceCRUDTest
        extends AbstractServiceCRUDTest<OAuthToken> {

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public OAuthTokenServiceCRUDTest(final ClientType clientType,
                                     final String tokenScope,
                                     final Boolean createUser,
                                     final Boolean shouldSucceed) {
        super(OAuthToken.class, clientType, tokenScope, createUser,
                shouldSucceed);
    }

    /**
     * Test parameters.
     *
     * @return List of parameters used to reconstruct this test.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.TOKEN,
                        true,
                        false
                },
                new Object[]{
                        ClientType.OwnerCredentials,
                        Scope.TOKEN_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.OwnerCredentials,
                        Scope.TOKEN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.OwnerCredentials,
                        Scope.TOKEN_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.OwnerCredentials,
                        Scope.TOKEN,
                        true,
                        false
                },
                new Object[]{
                        ClientType.AuthorizationGrant,
                        Scope.TOKEN_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.AuthorizationGrant,
                        Scope.TOKEN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.AuthorizationGrant,
                        Scope.TOKEN_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.AuthorizationGrant,
                        Scope.TOKEN,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.TOKEN_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.TOKEN,
                        false,
                        false
                });
    }

    /**
     * Create a valid token for a specific context.
     *
     * @param context The context to use for the token.
     * @param type    The type of token to create.
     * @return A bearer token.
     */
    private OAuthToken createValidToken(final ApplicationContext context,
                                        final OAuthTokenType type) {
        OAuthToken token = null;

        switch (type) {
            case Authorization:
                token = createAuthorizationToken(context);
                break;
            case Refresh:
                token = createRefreshToken(context);
                break;
            case Bearer:
            default:
                token = createBearerToken(context);
                break;
        }

        Session s = getSession();
        s.getTransaction().begin();
        s.save(token);
        s.getTransaction().commit();

        return token;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        UriBuilder builder = UriBuilder.fromPath("/token/");
        if (id != null) {
            builder.path(id);
        }
        return builder.build();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractEntity entity) {
        if (entity == null || entity.getId() == null) {
            return getUrlForId((String) null);
        }
        return getUrlForId(entity.getId().toString());
    }

    /**
     * Extract the appropriate entity from a provided context.
     *
     * @return The client currently active in the admin app.
     */
    @Override
    protected OAuthToken getEntity(final ApplicationContext context) {
        return getAttached(context.getToken());
    }

    /**
     * Create a brand new entity.
     *
     * @return A brand new entity!
     */
    @Override
    protected OAuthToken getNewEntity() {
        return new OAuthToken();
    }


    /**
     * Return the token scope required for admin access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.TOKEN_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.TOKEN;
    }

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected OAuthToken createValidEntity(final ApplicationContext context) {
        switch (getClientType()) {
            case AuthorizationGrant:
                return createAuthorizationToken(context);
            case Implicit:
                return createBearerToken(context);
            case ClientCredentials:
            case OwnerCredentials:
            default:
                return createBearerToken(context);
        }
    }

    /**
     * Create a valid authorization token in the present context.
     *
     * @param context The test application context.
     * @return A valid authorization token in the present context.
     */
    private OAuthToken createAuthorizationToken(
            final ApplicationContext context) {
        Client client = getAttached(context.getApplication()).getClients()
                .stream()
                .filter(c -> c.getType().equals(getClientType()))
                .collect(Collectors.toList())
                .get(0);

        // Get an identity from the client authenticator, if available.
        List<UserIdentity> identities = client.getAuthenticators().stream()
                .flatMap(ate -> ate.getIdentities().stream())
                .collect(Collectors.toList());


        // If there are redirects available...
        URI redirect = UriBuilder.fromPath("http://invalid.example.com/")
                .build();
        if (client.getRedirects().size() > 0) {
            redirect = client.getRedirects().iterator().next().getUri();
        }

        OAuthToken t = new OAuthToken();
        t.setClient(client);
        t.setTokenType(OAuthTokenType.Authorization);
        t.setRedirect(redirect);
        t.setExpiresIn(client.getAuthorizationCodeExpiresIn());

        if (identities.size() > 0) {
            t.setIdentity(identities.get(0));
        }

        return t;
    }

    /**
     * Create a valid refresh token in the present context.
     *
     * @param context The test application context.
     * @return A valid refresh token in the present context.
     */
    private OAuthToken createRefreshToken(
            final ApplicationContext context) {
        OAuthToken bearerToken = createBearerToken(context);

        OAuthToken token = new OAuthToken();
        token.setClient(bearerToken.getClient());
        token.setTokenType(OAuthTokenType.Refresh);
        token.setExpiresIn(bearerToken.getClient().getRefreshTokenExpireIn());
        token.setScopes(context.getScopes());
        token.setIdentity(bearerToken.getIdentity());

        return token;
    }

    /**
     * Create a valid bearer token in the present context.
     *
     * @param context The test application context.
     * @return A valid bearer token in the present context.
     */
    private OAuthToken createBearerToken(final ApplicationContext context) {
        // Get all clients of the correct type.
        List<Client> clients = getAttached(context.getApplication())
                .getClients()
                .stream()
                .filter(c -> c.getType().equals(getClientType()))
                .collect(Collectors.toList());

        // If we need an identity, further filter it down to a client that
        // has a user with an identity.
        if (!getClientType().equals(ClientType.ClientCredentials)) {
            clients = clients.stream()
                    .flatMap(c -> c.getAuthenticators().stream())
                    .flatMap(a -> a.getIdentities().stream())
                    .map(i -> i.getAuthenticator().getClient())
                    .collect(Collectors.toList());
        }

        // Grab the client.
        Client client = clients.get(0);

        // Get an identity from the client authenticator, if available.
        List<UserIdentity> identities = client.getAuthenticators().stream()
                .flatMap(ate -> ate.getIdentities().stream())
                .collect(Collectors.toList());

        OAuthToken token = new OAuthToken();
        token.setClient(client);
        token.setTokenType(OAuthTokenType.Bearer);
        token.setExpiresIn(client.getAccessTokenExpireIn());
        token.setScopes(context.getScopes());

        // Identities should only be added if the type calls for it.
        if (identities.size() > 0
                && !client.getType().equals(ClientType.ClientCredentials)) {
            token.setIdentity(identities.get(0));
        }

        return token;
    }

    /**
     * Assert you cannot create a token with an expiresIn that's zero or
     * smaller.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooSmallExpiresIn() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setExpiresIn(0);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert you cannot create a token with an expiresIn that's blank.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoExpiresIn() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setExpiresIn(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert you cannot create a token without a type.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoTokenType() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert that adding an identity fails/succeeds based on the client type.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostIdentity() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());

        // In some cases this won't be set.
        if (testEntity.getIdentity() == null) {
            testEntity.setIdentity(getAdminContext().getUserIdentity());
        }

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        if (shouldSucceed()
                && !getClientType().equals(ClientType.ClientCredentials)) {

            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            OAuthToken response = getResponse.readEntity(OAuthToken.class);
            assertContentEquals(testEntity, response);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that you cannot assign an identity from a different application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostIdentityFromWrongApp() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        UserIdentity otherIdentity = getSecondaryContext()
                .getBuilder()
                .identity()
                .build()
                .getUserIdentity();

        testEntity.setIdentity(otherIdentity);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create a refresh token with an identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostRefreshWithIdentity() throws Exception {
        OAuthToken bearerToken = createValidToken(getAdminContext(),
                OAuthTokenType.Bearer);
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Refresh);
        testEntity.setAuthToken(bearerToken);
        testEntity.setRedirect(null);

        // In some cases this won't be set.
        if (testEntity.getIdentity() == null) {
            testEntity.setIdentity(getAdminContext().getUserIdentity());
        }

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        if (shouldSucceed()
                && !getClientType().equals(ClientType.Implicit)
                && !getClientType().equals(ClientType.ClientCredentials)) {

            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            Assert.assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            OAuthToken response = getResponse.readEntity(OAuthToken.class);
            assertContentEquals(testEntity, response);
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Try to create a refresh token without an identity. This should always
     * return a bad request, because refresh tokens are only permitted in
     * identity-based flows.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostRefreshWithoutIdentity() throws Exception {
        OAuthToken bearerToken = createValidToken(getAdminContext(),
                OAuthTokenType.Bearer);
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Refresh);
        testEntity.setAuthToken(bearerToken);
        testEntity.setRedirect(null);
        testEntity.setIdentity(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create a refresh token with no bearer token.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostRefreshNoBearerToken() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Refresh);
        testEntity.setRedirect(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create a refresh token with an auth (not bearer) token.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostRefreshWithAuthToken() throws Exception {
        // Create a token first...
        ApplicationContext context = getAdminContext();
        OAuthToken authToken = createValidToken(context,
                OAuthTokenType.Authorization);

        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Refresh);
        testEntity.setAuthToken(authToken);
        testEntity.setRedirect(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create a refresh token with a bearer token that belongs to a
     * different identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostRefreshWrongBearerTokenParent() throws Exception {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .identity()
                .build();
        OAuthToken bearerToken = createValidToken(getAdminContext(),
                OAuthTokenType.Bearer);
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Refresh);
        testEntity.setAuthToken(bearerToken);
        testEntity.setIdentity(testContext.getUserIdentity());
        testEntity.setRedirect(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create a refresh token with a redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostRefreshWithRedirect() throws Exception {
        OAuthToken bearerToken = createValidToken(getAdminContext(),
                OAuthTokenType.Bearer);
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Refresh);
        testEntity.setAuthToken(bearerToken);
        if (testEntity.getRedirect() == null) {
            testEntity.setRedirect(UriBuilder
                    .fromPath("http://redirect.example.com/redirect")
                    .build());
        }

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create a bearer token with a redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostBearerWithRedirect() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Bearer);
        if (testEntity.getRedirect() == null) {
            testEntity.setRedirect(UriBuilder
                    .fromPath("http://redirect.example.com/redirect")
                    .build());
        }

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create a bearer token with a redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostBearerWithParentToken() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setTokenType(OAuthTokenType.Bearer);
        testEntity.setAuthToken(
                createValidToken(getAdminContext(),
                        OAuthTokenType.Bearer)
        );

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create an authorization token with an incorrect redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostAuthInvalidRedirect() throws Exception {
        OAuthToken testEntity = createValidEntity(getAdminContext());
        testEntity.setRedirect(UriBuilder
                .fromPath("http://invalid.example.com").build());

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Try to create an authorization token with no redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostAuthNoRedirect() throws Exception {
        // Make sure there's more than one redirect in this context.
        ApplicationContext testContext = getAdminContext().getBuilder()
                .redirect()
                .redirect()
                .build();
        OAuthToken testEntity = createValidEntity(testContext);
        testEntity.setTokenType(OAuthTokenType.Authorization);
        testEntity.setRedirect(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Assert you cannot update a token with an expiresIn field that's too
     * small.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutTooSmallExpiresIn() throws Exception {
        OAuthToken token = (OAuthToken) getAdminToken().clone();
        token.setExpiresIn(0);

        Response r = putEntity(token, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that you cannot update a token with a blank expiresIn field.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutNoExpiresIn() throws Exception {
        OAuthToken token = (OAuthToken) getAdminToken().clone();
        token.setExpiresIn(null);

        Response r = putEntity(token, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that you cannot update a token with no type.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutNoTokenType() throws Exception {
        OAuthToken token = (OAuthToken) getAdminToken().clone();
        token.setTokenType(null);

        Response r = putEntity(token, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Test a simple, valid put.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPut() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Bearer);
        testEntity.setExpiresIn(
                testEntity.getExpiresIn() + 100
        );

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
            OAuthToken response = r.readEntity(OAuthToken.class);
            assertContentEquals(testEntity, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that you cannot modify the identity of an existing token.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutIdentity() throws Exception {
        UserIdentity newIdentity = getAdminContext()
                .getBuilder()
                .identity()
                .build()
                .getUserIdentity();
        OAuthToken testEntity = getAdminToken();

        testEntity.setIdentity(newIdentity);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that you cannot assign an identity from a different application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutIdentityFromWrongApp() throws Exception {
        OAuthToken testEntity = getAdminToken();
        UserIdentity otherIdentity = getSecondaryContext()
                .getBuilder()
                .identity()
                .build()
                .getUserIdentity();

        testEntity.setIdentity(otherIdentity);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token with a different identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshWithIdentity() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);
        UserIdentity identity = getAdminContext()
                .getBuilder()
                .identity()
                .build()
                .getUserIdentity();
        testEntity.setIdentity(identity);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token without an identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshWithoutIdentity() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);
        testEntity.setIdentity(null);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token with a different client.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshWithClient() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);
        Client client = getAdminContext()
                .getBuilder()
                .client(ClientType.ClientCredentials)
                .build()
                .getClient();
        testEntity.setClient(client);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token without an identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshWithoutClient() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);
        testEntity.setClient(null);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token with no bearer token.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshNoBearerToken() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);
        testEntity.setAuthToken(null);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token with an auth (not bearer) token.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshWithAuthToken() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);
        OAuthToken authToken = createValidToken(getAdminContext(),
                OAuthTokenType.Authorization);
        testEntity.setAuthToken(authToken);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token with a bearer token that belongs to a
     * different identity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshWrongBearerTokenParent() throws Exception {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .identity()
                .build();
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);
        testEntity.setIdentity(testContext.getUserIdentity());
        testEntity.setRedirect(null);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update a refresh token with a redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRefreshWithRedirect() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Refresh);

        if (testEntity.getRedirect() == null) {
            testEntity.setRedirect(UriBuilder
                    .fromPath("http://redirect.example.com/redirect")
                    .build());
        }

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to create a bearer token with a redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutBearerWithRedirect() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Bearer);

        if (testEntity.getRedirect() == null) {
            testEntity.setRedirect(UriBuilder
                    .fromPath("http://redirect.example.com/redirect")
                    .build());
        }

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to create an authorization token with an incorrect redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutAuthInvalidRedirect() throws Exception {
        OAuthToken testEntity = createValidToken(getAdminContext(),
                OAuthTokenType.Authorization);
        testEntity.setRedirect(
                UriBuilder.fromPath("http://invalid.example.com/").build()
        );

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Try to update an authorization token with no redirect.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutAuthNoRedirect() throws Exception {
        ApplicationContext testContext = getAdminContext().getBuilder()
                .redirect()
                .redirect()
                .build();
        OAuthToken testEntity = createValidToken(testContext,
                OAuthTokenType.Authorization);
        testEntity.setRedirect(null);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
