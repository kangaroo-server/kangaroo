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

import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.filter.OAuth2;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.common.database.util.SortUtil;
import net.krotscheck.kangaroo.authz.common.util.ValidationUtil;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import org.apache.commons.lang.ObjectUtils;
import org.apache.lucene.search.Query;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.jvnet.hk2.annotations.Optional;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.UUID;

/**
 * A RESTful api that permits management of OAuth Tokens that were issued by
 * the ancillary servlets.
 *
 * @author Michael Krotscheck
 */
@Path("/token")
@RolesAllowed({Scope.TOKEN_ADMIN, Scope.TOKEN})
@OAuth2
@Transactional
public final class OAuthTokenService extends AbstractService {

    /**
     * Search the tokens in the system.
     *
     * @param offset         The offset of the first entity to fetch.
     * @param limit          The number of entities to fetch.
     * @param queryString    The search term for the query.
     * @param ownerId        An optional owner ID to filter by.
     * @param userId         An optional user ID to filter by.
     * @param userIdentityId An optional identity ID to filter by.
     * @param clientId       An optional client ID to filter by.
     * @param type           An optional OAuth Token Type to filter by.
     * @return A list of search results.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("CPD-START")
    public Response search(
            @DefaultValue("0") @QueryParam("offset") final Integer offset,
            @DefaultValue("10") @QueryParam("limit") final Integer limit,
            @DefaultValue("") @QueryParam("q") final String queryString,
            @Optional @QueryParam("owner") final UUID ownerId,
            @Optional @QueryParam("user") final UUID userId,
            @Optional @QueryParam("identity") final UUID userIdentityId,
            @Optional @QueryParam("client") final UUID clientId,
            @Optional @QueryParam("type") final OAuthTokenType type) {

        // Start a query builder...
        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(OAuthToken.class)
                .get();
        BooleanJunction junction = builder.bool();

        Query fuzzy = builder.keyword()
                .fuzzy()
                .onFields(new String[]{
                        "identity.remoteId",
                        "identity.claims"
                })
                .matching(queryString)
                .createQuery();
        junction = junction.must(fuzzy);

        // Attach an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            Query ownerQuery = builder
                    .keyword()
                    .onField("client.application.owner.id")
                    .matching(owner.getId())
                    .createQuery();
            junction.must(ownerQuery);
        }

        // Attach a user filter.
        User filterByUser = resolveFilterEntity(User.class, userId);
        if (filterByUser != null) {
            Query userQuery = builder
                    .keyword()
                    .onField("identity.user.id")
                    .matching(filterByUser.getId())
                    .createQuery();
            junction.must(userQuery);
        }

        // Attach an identity filter.
        UserIdentity filterByIdentity =
                resolveFilterEntity(UserIdentity.class, userIdentityId);
        if (filterByIdentity != null) {
            Query identityQuery = builder
                    .keyword()
                    .onField("identity.id")
                    .matching(filterByIdentity.getId())
                    .createQuery();
            junction.must(identityQuery);
        }

        // Attach a client filter.
        Client filterByClient =
                resolveFilterEntity(Client.class, clientId);
        if (filterByClient != null) {
            Query clientQuery = builder
                    .keyword()
                    .onField("client.id")
                    .matching(filterByClient.getId())
                    .createQuery();
            junction.must(clientQuery);
        }

        // Attach a type filter.
        if (type != null) {
            Query typeQuery = builder.keyword()
                    .onField("tokenType")
                    .matching(type)
                    .createQuery();
            junction.must(typeQuery);
        }

        FullTextQuery query = getFullTextSession()
                .createFullTextQuery(junction.createQuery(),
                        OAuthToken.class);

        return executeQuery(query, offset, limit);
    }

    /**
     * Browse the identities in the system.
     *
     * @param offset         The offset of the first entity to fetch.
     * @param limit          The number of entities to fetch.
     * @param sort           The field on which the entities should be sorted.
     * @param order          The sort order, ASC or DESC.
     * @param ownerId        An optional owner ID to filter by.
     * @param userIdentityId An optional identity ID to filter by.
     * @param clientId       An optional client ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response browse(
            @QueryParam(ApiParam.OFFSET_QUERY)
            @DefaultValue(ApiParam.OFFSET_DEFAULT) final int offset,
            @QueryParam(ApiParam.LIMIT_QUERY)
            @DefaultValue(ApiParam.LIMIT_DEFAULT) final int limit,
            @QueryParam(ApiParam.SORT_QUERY)
            @DefaultValue(ApiParam.SORT_DEFAULT) final String sort,
            @QueryParam(ApiParam.ORDER_QUERY)
            @DefaultValue(ApiParam.ORDER_DEFAULT) final SortOrder order,
            @Optional @QueryParam("owner") final UUID ownerId,
            @Optional @QueryParam("identity") final UUID userIdentityId,
            @Optional @QueryParam("client") final UUID clientId) {

        // Validate the incoming filters.
        User filterByOwner =
                resolveOwnershipFilter(ownerId);
        UserIdentity filterByIdentity =
                resolveFilterEntity(UserIdentity.class, userIdentityId);
        Client filterByClient =
                resolveFilterEntity(Client.class, clientId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession()
                .createCriteria(OAuthToken.class)
                .createAlias("client", "c")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(OAuthToken.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .createAlias("client", "c")
                .addOrder(SortUtil.order(order, sort));

        if (filterByClient != null) {
            browseCriteria
                    .add(Restrictions.eq("c.id", filterByClient.getId()));
            countCriteria
                    .add(Restrictions.eq("c.id", filterByClient.getId()));
        }

        if (filterByIdentity != null) {
            browseCriteria
                    .createAlias("identity", "i")
                    .add(Restrictions.eq("i.id", filterByIdentity.getId()));
            countCriteria
                    .createAlias("identity", "i")
                    .add(Restrictions.eq("i.id", filterByIdentity.getId()));
        }

        if (filterByOwner != null) {
            browseCriteria
                    .createAlias("c.application", "a")
                    .createAlias("a.owner", "o")
                    .add(Restrictions.eq("o.id", filterByOwner.getId()));
            countCriteria
                    .createAlias("c.application", "a")
                    .createAlias("a.owner", "o")
                    .add(Restrictions.eq("o.id", filterByOwner.getId()));
        }

        return ListResponseBuilder.builder()
                .offset(offset)
                .limit(limit)
                .order(order)
                .sort(sort)
                .total(countCriteria.uniqueResult())
                .addResult(browseCriteria.list())
                .build();
    }

    /**
     * Returns a specific entity.
     *
     * @param id The Unique Identifier for the entity.
     * @return A response with the entity that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(@PathParam("id") final UUID id) {
        OAuthToken token = getSession().get(OAuthToken.class, id);
        assertCanAccess(token, getAdminScope());
        return Response.ok(token).build();
    }

    /**
     * Create a new token.
     *
     * @param token The oauth token to create.
     * @return A redirect to the location where the identity was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final OAuthToken token) {

        OAuthToken validToken = validateInputData(token);

        // Validate that we have no ID.
        if (validToken.getId() != null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Assert that we can create a token in this application.
        Client parent = validToken.getClient();
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp = parent.getApplication();
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
        }

        // Yay, we're valid! Save it.
        Session s = getSession();
        s.save(validToken);
        s.getTransaction().commit();

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(OAuthTokenService.class, "getResource")
                .build(validToken.getId().toString());

        return Response.created(resourceLocation).build();
    }


    /**
     * Update a token.
     *
     * @param id    The Unique Identifier for the token.
     * @param token The token to update.
     * @return A response with the token that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("id") final UUID id,
                                   final OAuthToken token) {
        Session s = getSession();

        // Load the old instance.
        OAuthToken current = s.get(OAuthToken.class, id);

        // Make sure we can access this token.
        assertCanAccess(current, getAdminScope());

        // Make sure the body ID's match
        if (!current.equals(token)) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Make sure that we're not trying to change something we're not
        // permitted to change.
        if (!ObjectUtils.equals(current.getIdentity(), token.getIdentity())) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (!ObjectUtils.equals(current.getClient(), token.getClient())) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (!current.getTokenType().equals(token.getTokenType())) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (!ObjectUtils.equals(current.getAuthToken(), token.getAuthToken())) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Run all our other validations...
        OAuthToken validToken = validateInputData(token);

        // Copy over all the things we're allowed to edit.
        current.setExpiresIn(validToken.getExpiresIn());
        current.setRedirect(validToken.getRedirect());

        s.update(current);

        return Response.ok(current).build();
    }

    /**
     * Delete an scope.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response that indicates the success of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    public Response deleteResource(@PathParam("id") final UUID id) {
        Session s = getSession();
        OAuthToken token = s.get(OAuthToken.class, id);

        assertCanAccess(token, getAdminScope());

        // Let's hope they know what they're doing.
        s.delete(token);

        return Response.noContent().build();
    }

    /**
     * This method validates that an OAuthToken's referencing data is
     * entirely accurate, that all appropriate references are honored, and
     * that we're not trying to create a token with, say, an authenticator
     * from one application and a client from another.
     *
     * @param input The token to validate.
     * @return The same token, hydrated with validated data.
     */
    private OAuthToken validateInputData(final OAuthToken input) {
        // Validate that we have a body.
        if (input == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Validate that the token type is set.
        if (input.getTokenType() == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Validate that expiresIn is set and larger than zero.
        if (input.getExpiresIn() == null || input.getExpiresIn() < 1) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Validate that we have a valid client.
        Client client = requireEntityInput(Client.class, input.getClient());
        ClientType clientType = client.getType();
        input.setClient(client);

        // Validate that this particular client permits the creation of this
        // tokenType.
        if (clientType.equals(ClientType.OwnerCredentials)) {
            if (input.getTokenType().equals(OAuthTokenType.Authorization)) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
        } else if (clientType.in(ClientType.ClientCredentials,
                ClientType.Implicit)
                && !input.getTokenType().equals(OAuthTokenType.Bearer)) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Assert that we have a valid identity.
        if (clientType.equals(ClientType.ClientCredentials)) {
            if (input.getIdentity() != null) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
        } else {
            UserIdentity identity = requireEntityInput(UserIdentity.class,
                    input.getIdentity());
            if (!identity.getUser().getApplication()
                    .equals(client.getApplication())) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
            input.setIdentity(identity);
        }

        // Validate that if we're creating a refresh token, it must be linked
        // to an auth token. Otherwise it must not be.
        if (!input.getTokenType().equals(OAuthTokenType.Refresh)) {
            if (input.getAuthToken() != null) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
        } else {
            // Make sure we have a valid auth token.
            OAuthToken authToken = requireEntityInput(OAuthToken.class,
                    input.getAuthToken());

            // Make sure it's the correct type.
            if (!authToken.getTokenType().equals(OAuthTokenType.Bearer)) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }

            // Make sure it's for the correct user.
            if (!authToken.getIdentity().equals(input.getIdentity())) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }

            // It's valid, put it back.
            input.setAuthToken(authToken);
        }

        // Only authorization tokens use redirects.
        if (!input.getTokenType().equals(OAuthTokenType.Authorization)) {
            if (input.getRedirect() != null) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
        } else {
            URI redirect = ValidationUtil.validateRedirect(input.getRedirect(),
                    client.getRedirects());
            if (redirect == null) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
        }

        return input;
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.TOKEN_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.TOKEN;
    }
}
