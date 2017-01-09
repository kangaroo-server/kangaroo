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

import net.krotscheck.kangaroo.common.exception.exception.HttpForbiddenException;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.util.SortUtil;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2;
import org.apache.http.HttpStatus;
import org.hibernate.Criteria;
import org.hibernate.Session;
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
 * A RESTful API that permits the management of scope resources.
 *
 * @author Michael Krotscheck
 */
@Path("/scope")
@RolesAllowed({Scope.SCOPE, Scope.SCOPE_ADMIN})
@OAuth2
@Transactional
public final class ScopeService extends AbstractService {

    /**
     * Search the scopes in the system.
     *
     * @param offset        The offset of the first scopes to fetch.
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
            final UUID applicationId,
            @Optional @QueryParam("role")
            final UUID roleId) {

        FullTextQuery query = buildQuery(ApplicationScope.class,
                new String[]{"name"},
                queryString);

        // Attach an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            // Boolean switch on the owner ID.
            query.enableFullTextFilter("uuid_scope_owner")
                    .setParameter("indexPath", "application.owner.id")
                    .setParameter("uuid", owner.getId());
        }

        // Attach an application filter.
        Application filterByApp = resolveFilterEntity(
                Application.class,
                applicationId);
        if (filterByApp != null) {
            query.enableFullTextFilter("uuid_scope_application")
                    .setParameter("indexPath", "application.id")
                    .setParameter("uuid", filterByApp.getId());
        }

        return executeQuery(query, offset, limit);
    }

    /**
     * Browse the scopes in the system.
     *
     * @param offset        The offset of the first scopes to fetch.
     * @param limit         The number of data sets to fetch.
     * @param sort          The field on which the records should be sorted.
     * @param order         The sort order, ASC or DESC.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @param roleId        An optional role ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("CPD-START")
    public Response browseScopes(
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
                .createCriteria(ApplicationScope.class)
                .createAlias("application", "a")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(ApplicationScope.class)
                .createAlias("application", "a")
                .setFirstResult(offset)
                .setMaxResults(limit)
                .addOrder(SortUtil.order(order, sort));

        if (filterByApp != null) { // NOPMD - copy/paste
            browseCriteria.add(Restrictions.eq("a.id", filterByApp.getId()));
            countCriteria.add(Restrictions.eq("a.id", filterByApp.getId()));
        }

        if (filterByRole != null) { // NOPMD - copy/paste
            browseCriteria
                    .createAlias("roles", "r")
                    .add(Restrictions.eq("r.id", filterByRole.getId()));
            countCriteria
                    .createAlias("roles", "r")
                    .add(Restrictions.eq("r.id", filterByRole.getId()));
        }

        if (filterByOwner != null) { // NOPMD
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
     * Returns a specific scope.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response with the scope that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(@PathParam("id") final UUID id) {
        ApplicationScope scope = getSession().get(ApplicationScope.class, id);
        assertCanAccess(scope, Scope.SCOPE_ADMIN);
        return Response.ok(scope).build();
    }

    /**
     * Create an scope.
     *
     * @param scope The scope to create.
     * @return A response with the scope that was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final ApplicationScope scope) {

        // Input value checks.
        if (scope == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }
        if (scope.getId() != null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }
        if (scope.getApplication() == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Assert that we can create a scope in this application.
        if (!getSecurityContext().isUserInRole(Scope.SCOPE_ADMIN)) {
            Application scopeApp =
                    getSession().get(Application.class,
                            scope.getApplication().getId());
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
            }
        }

        // Save it all.
        Session s = getSession();
        s.save(scope);

        // Force a commit, to see what DB validation thinks of this.
        s.getTransaction().commit();

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(ScopeService.class, "getResource")
                .build(scope.getId().toString());

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an scope.
     *
     * @param id    The Unique Identifier for the scope.
     * @param scope The scope to update.
     * @return A response with the scope that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("id") final UUID id,
                                   final ApplicationScope scope) {
        Session s = getSession();

        // Load the old instance.
        ApplicationScope currentScope = s.get(ApplicationScope.class, id);

        assertCanAccess(currentScope, Scope.SCOPE_ADMIN);

        // Additional special case - we cannot modify the kangaroo app's scopes.
        if (currentScope.getApplication().equals(getAdminApplication())) {
            throw new HttpForbiddenException();
        }

        // Make sure the body ID's match
        if (!currentScope.equals(scope)) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Make sure we're not trying to change data we're not allowed.
        if (!currentScope.getApplication().equals(scope.getApplication())) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Transfer all the values we're allowed to edit.
        currentScope.setName(scope.getName());

        s.update(currentScope);

        return Response.ok(scope).build();
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
        ApplicationScope a = s.get(ApplicationScope.class, id);

        assertCanAccess(a, Scope.SCOPE_ADMIN);

        // Additional special case - we cannot delete the kangaroo app itself.
        if (a.getApplication().equals(getAdminApplication())) {
            throw new HttpForbiddenException();
        }

        // Let's hope they now what they're doing.
        s.delete(a);

        return Response.noContent().build();
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.SCOPE_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.SCOPE;
    }
}
