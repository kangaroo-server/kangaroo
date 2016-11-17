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
 * A RESTful API that permits the management of application role resources.
 *
 * @author Michael Krotscheck
 */
@Path("/role")
@RolesAllowed({Scope.ROLE, Scope.ROLE_ADMIN})
@OAuth2
public final class RoleService extends AbstractService {

    /**
     * Search the roles in the system.
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
    public Response search(
            @DefaultValue("0") @QueryParam("offset")
            final Integer offset,
            @DefaultValue("10") @QueryParam("limit")
            final Integer limit,
            @DefaultValue("") @QueryParam("q")
            final String queryString,
            @Optional @QueryParam("owner")
            final UUID ownerId,
            @Optional @QueryParam("application")
            final UUID applicationId) {

        FullTextQuery query = buildQuery(Role.class,
                new String[]{"name"},
                queryString);

        // Attach an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            // Boolean switch on the owner ID.
            query.enableFullTextFilter("uuid_role_owner")
                    .setParameter("indexPath", "application.owner.id")
                    .setParameter("uuid", owner.getId());
        }

        // Attach an application filter.
        Application filterByApp = resolveFilterEntity(
                Application.class,
                applicationId);
        if (filterByApp != null) {
            query.enableFullTextFilter("uuid_role_application")
                    .setParameter("indexPath", "application.id")
                    .setParameter("uuid", filterByApp.getId());
        }

        return executeQuery(query, offset, limit);
    }

    /**
     * Browse the roles in the system.
     *
     * @param offset        The offset of the first scopes to fetch.
     * @param limit         The number of data sets to fetch.
     * @param sort          The field on which the records should be sorted.
     * @param order         The sort order, ASC or DESC.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("CPD-START")
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
            final UUID applicationId) {

        // Validate the incoming filters.
        User filterByOwner = resolveOwnershipFilter(ownerId);
        Application filterByApp = resolveFilterEntity(
                Application.class,
                applicationId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession()
                .createCriteria(Role.class)
                .createAlias("application", "a")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(Role.class)
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
     * Returns a specific role.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response with the scope that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(@PathParam("id") final UUID id) {
        Role role = getSession().get(Role.class, id);
        assertCanAccess(role, getAdminScope());
        return Response.ok(role).build();
    }

    /**
     * Create an role.
     *
     * @param role The role to create.
     * @return A redirect to the location where the role was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final Role role) {

        // Input value checks.
        if (role == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }
        if (role.getId() != null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }
        if (role.getApplication() == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Assert that we can create a scope in this application.
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp =
                    getSession().get(Application.class,
                            role.getApplication().getId());
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
            }
        }

        // Save it all.
        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.save(role);
        t.commit();

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(RoleService.class, "getResource")
                .build(role.getId().toString());

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an role.
     *
     * @param id   The Unique Identifier for the role.
     * @param role The role to update.
     * @return A response with the role that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("id") final UUID id,
                                   final Role role) {
        Session s = getSession();

        // Load the old instance.
        Role current = s.get(Role.class, id);

        assertCanAccess(current, getAdminScope());

        // Make sure the body ID's match
        if (!current.equals(role)) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // You cannot modify a role from the admin application.
        if (current.getApplication().equals(getAdminApplication())) {
            throw new HttpStatusException(HttpStatus.SC_FORBIDDEN);
        }

        // Make sure we're not trying to change the parent entity.
        if (!current.getApplication().equals(role.getApplication())) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Transfer all the values we're allowed to edit.
        current.setName(role.getName());

        Transaction t = s.beginTransaction();
        s.update(current);
        t.commit();

        return Response.ok(role).build();
    }

    /**
     * Delete a role.
     *
     * @param id The Unique Identifier for the role.
     * @return A response that indicates the successs of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    public Response deleteResource(@PathParam("id") final UUID id) {
        Session s = getSession();
        Role role = s.get(Role.class, id);

        assertCanAccess(role, getAdminScope());

        // You cannot delete a role from the admin application.
        if (role.getApplication().equals(getAdminApplication())) {
            throw new HttpStatusException(HttpStatus.SC_FORBIDDEN);
        }

        // Let's hope they now what they're doing.
        Transaction t = s.beginTransaction();
        s.delete(role);
        t.commit();

        return Response.noContent().build();
    }

    /**
     * Expose a subresource that manages the scopes on a role. Note that the
     * OAuth2 flow will not be initialized until the path fully resolves, so
     * all auth checks have to happen in the child resource.
     *
     * @param roleId The ID of the role.
     * @return The subresource.
     */
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}"
            + "/scope/")
    public RoleScopeService getScopeService(
            @PathParam("id") final UUID roleId) {

        // Build a new role scope service.
        RoleScopeService scopeService = getServiceLocator()
                .getService(RoleScopeService.class);
        scopeService.setRoleId(roleId);

        return scopeService;
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override

    protected String getAdminScope() {
        return Scope.ROLE_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.ROLE;
    }
}
