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
import net.krotscheck.kangaroo.authz.admin.v1.exception.EntityRequiredException;
import net.krotscheck.kangaroo.authz.admin.v1.exception.InvalidEntityPropertyException;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
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
 * A RESTful API that permits the management of user resources.
 *
 * @author Michael Krotscheck
 */
@Path("/user")
@ScopesAllowed({Scope.USER, Scope.USER_ADMIN})
@Transactional
@Api(tags = "User",
     authorizations = {
             @Authorization(value = "Kangaroo", scopes = {
                     @AuthorizationScope(
                             scope = Scope.USER,
                             description = "Modify users in one"
                                     + " application."),
                     @AuthorizationScope(
                             scope = Scope.USER_ADMIN,
                             description = "Modify users in all"
                                     + " applications.")
             })
     })
public final class UserService extends AbstractService {

    /**
     * Search the users in the system.
     *
     * @param offset        The offset of the first users to fetch.
     * @param limit         The number of data sets to fetch.
     * @param queryString   The search term for the query.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @param roleId        An optional role ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search users")
    @SuppressWarnings({"CPD-START"})
    public Response searchUsers(
            @DefaultValue("0") @QueryParam("offset") final Integer offset,
            @DefaultValue("10") @QueryParam("limit") final Integer limit,
            @DefaultValue("") @QueryParam("q") final String queryString,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("owner") final BigInteger ownerId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("application") final BigInteger applicationId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("role") final BigInteger roleId) {

        // Start a query builder...
        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(User.class)
                .get();
        BooleanJunction junction = builder.bool();

        Query fuzzy = builder.keyword()
                .fuzzy()
                .onFields(new String[]{
                        "identities.claims",
                        "identities.remoteId"
                })
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
            Query userQuery = builder
                    .keyword()
                    .onField("application.id")
                    .matching(filterByApp.getId())
                    .createQuery();
            junction.must(userQuery);
        }

        // Attach an role filter.
        Role filterByRole = resolveFilterEntity(Role.class, roleId);
        if (filterByRole != null) {
            Query userQuery = builder
                    .keyword()
                    .onField("role.id")
                    .matching(filterByRole.getId())
                    .createQuery();
            junction.must(userQuery);
        }

        FullTextQuery query = getFullTextSession()
                .createFullTextQuery(junction.createQuery(),
                        User.class);

        return executeQuery(User.class, query, offset, limit);
    }

    /**
     * Returns a list of all users.
     *
     * @param offset        Paging offset, in records.
     * @param limit         The number of records to return.
     * @param sort          The field on which the records should be sorted.
     * @param order         The sort order, ASC or DESC.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @param roleId        An optional role ID to filter by.
     * @return A response with the results of the passed parameters.
     */
    @SuppressWarnings({"CPD-END"})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Browse users")
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
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("role") final BigInteger roleId) {

        // Validate the incoming filters.
        User filterByOwner = resolveOwnershipFilter(ownerId);
        Application filterByApp = resolveFilterEntity(
                Application.class,
                applicationId);
        Role filterByRole = resolveFilterEntity(
                Role.class,
                roleId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession()
                .createCriteria(User.class)
                .createAlias("application", "a")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(User.class)
                .createAlias("application", "a")
                .setFirstResult(offset)
                .setMaxResults(limit)
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

        if (filterByRole != null) {
            browseCriteria
                    .createAlias("role", "r")
                    .add(Restrictions.eq("r.id", filterByRole.getId()));
            countCriteria
                    .createAlias("role", "r")
                    .add(Restrictions.eq("r.id", filterByRole.getId()));
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
     * Returns a specific user.
     *
     * @param id The Unique Identifier for the user.
     * @return A response with the scope that was requested.
     */
    @GET
    @Path("/{id: [a-f0-9]{32}}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read user")
    public Response getResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id) {
        User scope = getSession().get(User.class, id);
        assertCanAccess(scope, getAdminScope());
        return Response.ok(scope).build();
    }

    /**
     * Create an user.
     *
     * @param user The user to create.
     * @return A response with the scope that was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create user")
    public Response createResource(final User user) {

        // Input value checks.
        if (user == null) {
            throw new EntityRequiredException();
        }
        if (user.getId() != null) {
            throw new InvalidEntityPropertyException("id");
        }
        if (user.getApplication() == null) {
            throw new InvalidEntityPropertyException("application");
        }

        // Assert that we can create a scope in this application.
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp =
                    getSession().get(Application.class,
                            user.getApplication().getId());
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new BadRequestException();
            }
        }

        // Save it all.
        Session s = getSession();
        s.save(user);

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(UserService.class, "getResource")
                .build(IdUtil.toString(user.getId()));

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an user.
     *
     * @param id   The Unique Identifier for the user.
     * @param user The user to update.
     * @return A response with the scope that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{32}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update user")
    public Response updateResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id,
            final User user) {
        Session s = getSession();

        // Load the old instance.
        User currentUser = s.get(User.class, id);

        assertCanAccess(currentUser, getAdminScope());

        // Make sure the body ID's match
        if (!currentUser.equals(user)) {
            throw new InvalidEntityPropertyException("id");
        }

        // Make sure we're not trying to change data we're not allowed.
        if (!currentUser.getApplication().equals(user.getApplication())) {
            throw new InvalidEntityPropertyException("application");
        }

        // Transfer all the values we're allowed to edit.
        currentUser.setRole(user.getRole());

        s.update(currentUser);

        return Response.ok(user).build();
    }

    /**
     * Delete a user.
     *
     * @param id The Unique Identifier for the user.
     * @return A response that indicates the successs of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{32}}")
    @ApiOperation(value = "Delete user")
    public Response deleteResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id) {
        Session s = getSession();
        User user = s.get(User.class, id);

        assertCanAccess(user, getAdminScope());

        // Let's hope they now what they're doing.
        s.delete(user);

        return Response.status(Status.RESET_CONTENT).build();
    }

    /**
     * The access scope required as an admin.
     *
     * @return The scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.USER_ADMIN;
    }

    /**
     * The access scope required as a regular user.
     *
     * @return The scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.USER;
    }
}
