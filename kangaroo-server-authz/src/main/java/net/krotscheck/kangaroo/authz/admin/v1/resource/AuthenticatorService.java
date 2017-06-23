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
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.util.SortUtil;
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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
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
import java.net.URI;
import java.util.UUID;

/**
 * A RESTful API that permits the management of client authentication resources.
 *
 * @author Michael Krotscheck
 */
@Path("/authenticator")
@RolesAllowed({Scope.AUTHENTICATOR, Scope.AUTHENTICATOR_ADMIN})
@OAuth2
@Transactional
public final class AuthenticatorService extends AbstractService {

    /**
     * Search the authenticators in the system.
     *
     * @param offset      The offset of the first entity to fetch.
     * @param limit       The number of entities to fetch.
     * @param queryString The search term for the query.
     * @param ownerId     An optional user ID to filter by.
     * @param clientId    An optional client ID to filter by.
     * @param type        An optional authenticator type to filter by.
     * @return A list of search results.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings({"CPD-START"})
    public Response search(
            @DefaultValue("0") @QueryParam("offset") final Integer offset,
            @DefaultValue("10") @QueryParam("limit") final Integer limit,
            @DefaultValue("") @QueryParam("q") final String queryString,
            @Optional @QueryParam("owner") final UUID ownerId,
            @Optional @QueryParam("client") final UUID clientId,
            @Optional @QueryParam("type") final AuthenticatorType type) {

        // Start a query builder...
        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Authenticator.class)
                .get();
        BooleanJunction junction = builder.bool();

        // Search on the client name.
        Query fuzzy = builder.keyword()
                .fuzzy()
                .onFields(new String[]{"client.name"})
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

        // Attach a client filter.
        Client filterByClient = resolveFilterEntity(Client.class, clientId);
        if (filterByClient != null) {
            Query userQuery = builder
                    .keyword()
                    .onField("client.id")
                    .matching(filterByClient.getId())
                    .createQuery();
            junction.must(userQuery);
        }

        // Attach a type filter.
        if (type != null) {
            Query typeQuery = builder.keyword()
                    .onField("type")
                    .matching(type)
                    .createQuery();
            junction.must(typeQuery);
        }

        FullTextQuery query = getFullTextSession()
                .createFullTextQuery(junction.createQuery(),
                        Authenticator.class);

        return executeQuery(query, offset, limit);
    }

    /**
     * Browse the authenticators in the system.
     *
     * @param offset   The offset of the first entity to fetch.
     * @param limit    The number of entities to fetch.
     * @param sort     The field on which the entities should be sorted.
     * @param order    The sort order, ASC or DESC.
     * @param ownerId  An optional user ID to filter by.
     * @param clientId An optional client ID to filter by.
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
            @Optional @QueryParam("client") final UUID clientId) {

        // Validate the incoming filters.
        User filterByOwner = resolveOwnershipFilter(ownerId);
        Client filterByClient = resolveFilterEntity(
                Client.class,
                clientId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession()
                .createCriteria(Authenticator.class)
                .createAlias("client", "c")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(Authenticator.class)
                .createAlias("client", "c")
                .setFirstResult(offset)
                .setMaxResults(limit)
                .addOrder(SortUtil.order(order, sort));

        if (filterByClient != null) {
            browseCriteria.add(Restrictions.eq("c.id", filterByClient.getId()));
            countCriteria.add(Restrictions.eq("c.id", filterByClient.getId()));
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
     * @param id The Unique Identifier for the scope.
     * @return A response with the scope that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(@PathParam("id") final UUID id) {
        Authenticator authenticator = getSession().get(Authenticator.class, id);
        assertCanAccess(authenticator, getAdminScope());
        return Response.ok(authenticator).build();
    }

    /**
     * Create a new authenticator.
     *
     * @param authenticator The authenticator to create.
     * @return A redirect to the location where the authenticator was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final Authenticator authenticator) {

        // Input value checks.
        if (authenticator == null) {
            throw new BadRequestException();
        }
        if (authenticator.getId() != null) {
            throw new BadRequestException();
        }
        if (authenticator.getClient() == null) {
            throw new BadRequestException();
        }
        if (authenticator.getType() == null) {
            throw new BadRequestException();
        }
        Client parent = getSession().get(Client.class,
                authenticator.getClient().getId());
        if (parent == null) {
            throw new BadRequestException();
        }

        // Assert that we can create an authenticator in this application.
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp = parent.getApplication();
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new BadRequestException();
            }
        }

        // Save it all.
        Session s = getSession();
        s.save(authenticator);

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(AuthenticatorService.class, "getResource")
                .build(authenticator.getId().toString());

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an authenticator.
     *
     * @param id            The Unique Identifier for the authenticator.
     * @param authenticator The authenticator to update.
     * @return A response with the authenticator that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("id") final UUID id,
                                   final Authenticator authenticator) {
        Session s = getSession();

        // Load the old instance.
        Authenticator current = s.get(Authenticator.class, id);

        assertCanAccess(current, getAdminScope());

        // Make sure the body ID's match
        if (!current.equals(authenticator)) {
            throw new BadRequestException();
        }

        // Make sure we're not trying to change the parent entity.
        if (!current.getClient().equals(authenticator.getClient())) {
            throw new BadRequestException();
        }

        // Make sure the type isn't void.
        if (authenticator.getType() == null) {
            throw new BadRequestException();
        }

        // Transfer all the values we're allowed to edit.
        current.setType(authenticator.getType());
        current.setConfiguration(authenticator.getConfiguration());

        s.update(current);

        return Response.ok(current).build();
    }

    /**
     * Delete an scope.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response that indicates the successs of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    public Response deleteResource(@PathParam("id") final UUID id) {
        Session s = getSession();
        Authenticator authenticator = s.get(Authenticator.class, id);

        assertCanAccess(authenticator, getAdminScope());

        // Let's hope they know what they're doing.
        s.delete(authenticator);

        return Response.noContent().build();
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.AUTHENTICATOR_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.AUTHENTICATOR;
    }
}
