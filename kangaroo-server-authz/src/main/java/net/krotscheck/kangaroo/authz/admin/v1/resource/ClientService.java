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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.auth.ScopesAllowed;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.util.SortUtil;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import org.apache.lucene.search.Query;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.jvnet.hk2.annotations.Optional;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
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
import java.math.BigInteger;
import java.net.URI;


/**
 * A RESTful API that permits the management of application client resources.
 *
 * @author Michael Krotscheck
 */
@Path("/client")
@ScopesAllowed({Scope.CLIENT, Scope.CLIENT_ADMIN})
@Transactional
@Api(tags = "Client",
        authorizations = {
                @Authorization(value = "Kangaroo", scopes = {
                        @AuthorizationScope(
                                scope = Scope.CLIENT,
                                description = "Modify clients in one"
                                        + " application."),
                        @AuthorizationScope(
                                scope = Scope.CLIENT_ADMIN,
                                description = "Modify clients in all"
                                        + " applications.")
                })
        })
public final class ClientService extends AbstractService {

    /**
     * Search the clients in the system.
     *
     * @param offset        The offset of the first scopes to fetch.
     * @param limit         The number of data sets to fetch.
     * @param queryString   The search term for the query.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search clients")
    @SuppressWarnings({"CPD-START"})
    public Response search(
            @DefaultValue("0") @QueryParam("offset") final Integer offset,
            @DefaultValue("10") @QueryParam("limit") final Integer limit,
            @DefaultValue("") @QueryParam("q") final String queryString,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("owner") final BigInteger ownerId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("application")
            final BigInteger applicationId) {

        // Start a query builder...
        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Client.class)
                .get();
        BooleanJunction junction = builder.bool();

        Query fuzzy = builder.keyword()
                .fuzzy()
                .onFields(new String[]{"name"})
                .matching(queryString)
                .createQuery();
        junction = junction.must(fuzzy);

        // Attach an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            Query ownerQuery = builder
                    .keyword()
                    .onField("application.owner.id")
                    .matching(owner.getId())
                    .createQuery();
            junction.must(ownerQuery);
        }

        // Attach an application filter.
        Application filterByApp = resolveFilterEntity(Application.class,
                applicationId);
        if (filterByApp != null) {
            Query appQuery = builder
                    .keyword()
                    .onField("application.id")
                    .matching(filterByApp.getId())
                    .createQuery();
            junction.must(appQuery);
        }

        FullTextQuery query = getFullTextSession()
                .createFullTextQuery(junction.createQuery(),
                        Client.class);

        return executeQuery(Client.class, query, offset, limit);
    }

    /**
     * Browse the clients in the system.
     *
     * @param offset        The offset of the first scopes to fetch.
     * @param limit         The number of data sets to fetch.
     * @param sort          The field on which the records should be sorted.
     * @param order         The sort order, ASC or DESC.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @param clientType    An optional client type to filter by.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Browse clients")
    public Response browse(
            @QueryParam(ApiParam.OFFSET_QUERY)
            @DefaultValue(ApiParam.OFFSET_DEFAULT) final int offset,
            @QueryParam(ApiParam.LIMIT_QUERY)
            @DefaultValue(ApiParam.LIMIT_DEFAULT) final int limit,
            @QueryParam(ApiParam.SORT_QUERY)
            @DefaultValue(ApiParam.SORT_DEFAULT) final String sort,
            @QueryParam(ApiParam.ORDER_QUERY)
            @DefaultValue(ApiParam.ORDER_DEFAULT) final SortOrder order,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("owner") final BigInteger ownerId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("application") final BigInteger applicationId,
            @Optional @QueryParam("type") final ClientType clientType) {

        // Validate the incoming filters.
        User filterByOwner = resolveOwnershipFilter(ownerId);
        Application filterByApp = resolveFilterEntity(
                Application.class,
                applicationId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession()
                .createCriteria(Client.class)
                .createAlias("application", "a")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(Client.class)
                .createAlias("application", "a")
                .setFirstResult(offset)
                .setMaxResults(limit)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .addOrder(SortUtil.order(order, sort));

        if (filterByApp != null) {
            browseCriteria.add(Restrictions.eq("a.id", filterByApp.getId()));
            countCriteria.add(Restrictions.eq("a.id", filterByApp.getId()));
        }

        if (filterByOwner != null) {
            browseCriteria
                    .createAlias("a.owner", "o")
                    .add(Restrictions.eq("o.id", filterByOwner.getId()));
            countCriteria
                    .createAlias("a.owner", "o")
                    .add(Restrictions.eq("o.id", filterByOwner.getId()));
        }

        if (clientType != null) {
            browseCriteria.add(Restrictions.eq("type", clientType));
            countCriteria.add(Restrictions.eq("type", clientType));
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
     * Returns a specific scope.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response with the scope that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{32}}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read client")
    public Response getResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id) {
        Client client = getSession().get(Client.class, id);
        assertCanAccess(client, getAdminScope());
        return Response.ok(client).build();
    }

    /**
     * Create an client.
     *
     * @param client The client to create.
     * @return A redirect to the location where the client was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create client")
    public Response createResource(final Client client) {

        // Input value checks.
        if (client == null) {
            throw new BadRequestException();
        }
        if (client.getId() != null) {
            throw new BadRequestException();
        }
        if (client.getApplication() == null) {
            throw new BadRequestException();
        }

        // Assert that we can create a scope in this application.
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp =
                    getSession().get(Application.class,
                            client.getApplication().getId());
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new BadRequestException();
            }
        }

        // Save it all.
        Session s = getSession();
        s.save(client);

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(ClientService.class, "getResource")
                .build(IdUtil.toString(client.getId()));

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an client.
     *
     * @param id     The Unique Identifier for the client.
     * @param client The client to update.
     * @return A response with the client that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{32}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update client")
    public Response updateResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id,
            final Client client) {
        Session s = getSession();

        // Load the old instance.
        Client current = s.get(Client.class, id);

        assertCanAccess(current, getAdminScope());

        // Make sure the body ID's match
        if (!current.equals(client)) {
            throw new BadRequestException();
        }

        // Additional special case: We cannot modify the client we're
        // currently using to access this api.
        OAuthToken token = (OAuthToken) getSecurityContext().getUserPrincipal();
        if (token.getClient().equals(client)) {
            throw new ClientErrorException(Status.CONFLICT);
        }


        // Make sure we're not trying to change the parent entity.
        if (!current.getApplication().equals(client.getApplication())) {
            throw new BadRequestException();
        }

        // Transfer all the values we're allowed to edit.
        current.setName(client.getName());
        current.setType(client.getType());
        current.setClientSecret(client.getClientSecret());

        s.update(current);

        return Response.ok(client).build();
    }

    /**
     * Delete an scope.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response that indicates the successs of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{32}}")
    @ApiOperation(value = "Delete client")
    public Response deleteResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id) {
        Session s = getSession();
        Client client = s.get(Client.class, id);

        assertCanAccess(client, getAdminScope());

        // Additional special case: We cannot modify the client we're
        // currently using to access this api.
        OAuthToken token = (OAuthToken) getSecurityContext().getUserPrincipal();
        if (token.getClient().equals(client)) {
            throw new ClientErrorException(Status.CONFLICT);
        }

        // Let's hope they now what they're doing.
        s.delete(client);

        return Response.noContent().build();
    }

    /**
     * Expose a subresource that manages the redirects on a client. Note that
     * the OAuth2 flow will not be initialized until the path fully resolves, so
     * all auth checks have to happen in the child resource.
     *
     * @param clientId The ID of the client.
     * @return The subresource.
     */
    @Path("/{clientId: [a-f0-9]{32}}/redirect")
    public Class<ClientRedirectService> getRedirectService(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("clientId") final BigInteger clientId) {
        return ClientRedirectService.class;
    }

    /**
     * Expose a subresource that manages the referrers on a client. Note that
     * the OAuth2 flow will not be initialized until the path fully resolves, so
     * all auth checks have to happen in the child resource.
     *
     * @param clientId The ID of the client.
     * @return The subresource.
     */
    @Path("/{clientId: [a-f0-9]{32}}/referrer")
    public Class<ClientReferrerService> getReferrerService(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("clientId") final BigInteger clientId) {
        return ClientReferrerService.class;
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.CLIENT_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.CLIENT;
    }
}
