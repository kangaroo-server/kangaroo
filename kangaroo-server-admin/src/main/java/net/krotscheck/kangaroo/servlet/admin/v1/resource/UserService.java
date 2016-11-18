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

import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.util.SortUtil;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2;
import org.apache.http.HttpStatus;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
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
import java.net.URI;
import java.util.UUID;

/**
 * A RESTful API that permits the management of user resources.
 *
 * @author Michael Krotscheck
 */
@Path("/user")
@RolesAllowed({Scope.USER, Scope.USER_ADMIN})
@OAuth2
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
    public Response searchUsers(
            @DefaultValue("0") @QueryParam("offset")
            final Integer offset,
            @DefaultValue("10") @QueryParam("limit")
            final Integer limit,
            @DefaultValue("") @QueryParam("q")
            final String queryString,
            @Optional @QueryParam("owner")
            final UUID ownerId,
            @Optional @QueryParam("application")
            final UUID applicationId,
            @Optional @QueryParam("role")
            final UUID roleId) {

        FullTextQuery query = buildQuery(User.class,
                new String[]{"identities.claims", "identities.remoteId"},
                queryString);

        // Attach an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            // Boolean switch on the owner ID.
            query.enableFullTextFilter("uuid_user_owner")
                    .setParameter("indexPath", "application.owner.id")
                    .setParameter("uuid", owner.getId());
        }

        // Attach an application filter.
        Application filterByApp = resolveFilterEntity(
                Application.class,
                applicationId);
        if (filterByApp != null) {
            query.enableFullTextFilter("uuid_user_application")
                    .setParameter("indexPath", "application.id")
                    .setParameter("uuid", filterByApp.getId());
        }

        // Attach a role filter.
        Role filterByRole = resolveFilterEntity(Role.class, roleId);
        if (filterByRole != null) {
            query.enableFullTextFilter("uuid_user_role")
                    .setParameter("indexPath", "role.id")
                    .setParameter("uuid", filterByRole.getId());
        }

        return executeQuery(query, offset, limit);
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
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response browse(
            @QueryParam(ApiParam.OFFSET_QUERY)
            @DefaultValue(ApiParam.OFFSET_DEFAULT)
            final int offset,
            @QueryParam(ApiParam.LIMIT_QUERY)
            @DefaultValue(ApiParam.LIMIT_DEFAULT)
            final int limit,
            @QueryParam(ApiParam.SORT_QUERY)
            @DefaultValue(ApiParam.SORT_DEFAULT)
            final String sort,
            @QueryParam(ApiParam.ORDER_QUERY)
            @DefaultValue(ApiParam.ORDER_DEFAULT)
            final SortOrder order,
            @Optional @QueryParam("owner")
            final UUID ownerId,
            @Optional @QueryParam("application")
            final UUID applicationId,
            @Optional @QueryParam("role")
            final UUID roleId) {

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
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(@PathParam("id") final UUID id) {
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
    public Response createResource(final User user) {

        // Input value checks.
        if (user == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }
        if (user.getId() != null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }
        if (user.getApplication() == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Assert that we can create a scope in this application.
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp =
                    getSession().get(Application.class,
                            user.getApplication().getId());
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
            }
        }

        // Save it all.
        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.save(user);
        t.commit();

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(UserService.class, "getResource")
                .build(user.getId().toString());

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
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("id") final UUID id,
                                   final User user) {
        Session s = getSession();

        // Load the old instance.
        User currentUser = s.get(User.class, id);

        assertCanAccess(currentUser, getAdminScope());

        // Make sure the body ID's match
        if (!currentUser.equals(user)) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Make sure we're not trying to change data we're not allowed.
        if (!currentUser.getApplication().equals(user.getApplication())) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Transfer all the values we're allowed to edit.
        currentUser.setRole(user.getRole());

        Transaction t = s.beginTransaction();
        s.update(currentUser);
        t.commit();

        return Response.ok(user).build();
    }

    /**
     * Delete a user.
     *
     * @param id The Unique Identifier for the user.
     * @return A response that indicates the successs of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    public Response deleteResource(@PathParam("id") final UUID id) {
        Session s = getSession();
        User user = s.get(User.class, id);

        assertCanAccess(user, getAdminScope());

        // Let's hope they now what they're doing.
        Transaction t = s.beginTransaction();
        s.delete(user);
        t.commit();

        return Response.noContent().build();
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
