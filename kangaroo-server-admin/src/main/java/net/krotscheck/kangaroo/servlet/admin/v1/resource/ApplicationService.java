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
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import net.krotscheck.kangaroo.database.entity.Application;
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
 * A RESTful API that permits the management of application resources.
 *
 * @author Michael Krotscheck
 */
@Path("/application")
@RolesAllowed({Scope.APPLICATION, Scope.APPLICATION_ADMIN})
@OAuth2
public final class ApplicationService extends AbstractService {

    /**
     * Search the applications in the system.
     *
     * @param offset      The offset of the first applications to fetch.
     * @param limit       The number of data sets to fetch.
     * @param queryString The search term for the query.
     * @param ownerId     An optional user ID to filter by.
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
            final UUID ownerId) {

        FullTextQuery query = buildQuery(Application.class,
                new String[]{"name"},
                queryString);

        // Add an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            // Boolean switch on the owner ID.
            query.enableFullTextFilter("uuid_application_owner")
                    .setParameter("indexPath", "owner.id")
                    .setParameter("uuid", owner.getId());
        }

        return executeQuery(query, offset, limit);
    }

    /**
     * Browse the applications in the system.
     *
     * @param offset  The offset of the first applications to fetch.
     * @param limit   The number of data sets to fetch.
     * @param sort    The field on which the records should be sorted.
     * @param order   The sort order, ASC or DESC.
     * @param ownerId An optional user ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response browseApplications(
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
            final UUID ownerId) {
        // Validate the incoming owner id.
        User owner = resolveOwnershipFilter(ownerId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession().createCriteria(Application.class);
        countCriteria.setProjection(Projections.rowCount());

        Criteria browseCriteria =
                getSession().createCriteria(Application.class);
        browseCriteria.setFirstResult(offset);
        browseCriteria.setMaxResults(limit);
        browseCriteria.addOrder(SortUtil.order(order, sort));

        if (owner != null) {
            // Boolean switch on the owner ID.
            browseCriteria.add(Restrictions.eq("owner", owner));
            countCriteria.add(Restrictions.eq("owner", owner));
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
     * Returns a specific application.
     *
     * @param id The Unique Identifier for the application.
     * @return A response with the application that was requested.
     */
    @GET
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(@PathParam("id") final UUID id) {
        Application application = getSession().get(Application.class, id);
        assertCanAccess(application, Scope.APPLICATION_ADMIN);
        return Response.ok(application).build();
    }

    /**
     * Create an application.
     *
     * @param application The application to create.
     * @return A response with the application that was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final Application application) {
        // Validate that the ID is empty.
        if (application == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }
        if (application.getId() != null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Only admins can change the owner.
        if (application.getOwner() != null) {
            if (!getSecurityContext().isUserInRole(Scope.APPLICATION_ADMIN)
                    && !application.getOwner().equals(getCurrentUser())) {
                throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
            }
        } else if (getCurrentUser() == null) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        } else {
            application.setOwner(getCurrentUser());
        }

        // Save it all.
        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.save(application);
        t.commit();

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(ApplicationService.class, "getResource")
                .build(application.getId().toString());

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an application.
     *
     * @param id          The Unique Identifier for the application.
     * @param application The application to update.
     * @return A response with the application that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("id") final UUID id,
                                   final Application application) {
        Session s = getSession();

        // Load the old instance.
        Application currentApp = s.get(Application.class, id);

        assertCanAccess(currentApp, Scope.APPLICATION_ADMIN);

        // Additional special case - we cannot modify the kangaroo app itself.
        if (currentApp.equals(getAdminApplication())) {
            throw new HttpForbiddenException();
        }

        // Make sure the body ID's match
        if (!currentApp.equals(application)) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Make sure we're not trying to change data we're not allowed.
        if (!currentApp.getOwner().equals(application.getOwner())) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // Transfer all the values we're allowed to edit.
        currentApp.setName(application.getName());

        Transaction t = s.beginTransaction();
        s.update(currentApp);
        t.commit();

        return Response.ok(application).build();
    }

    /**
     * Delete an application.
     *
     * @param id The Unique Identifier for the application.
     * @return A response that indicates the successs of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    public Response deleteResource(@PathParam("id") final UUID id) {
        Session s = getSession();
        Application a = s.get(Application.class, id);

        assertCanAccess(a, Scope.APPLICATION_ADMIN);

        // Additional special case - we cannot delete the kangaroo app itself.
        if (a.equals(getAdminApplication())) {
            throw new HttpForbiddenException();
        }

        // Let's hope they now what they're doing.
        Transaction t = s.beginTransaction();
        s.delete(a);
        t.commit();

        return Response.noContent().build();
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.APPLICATION_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.APPLICATION;
    }
}
