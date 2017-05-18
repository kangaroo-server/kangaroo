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

import com.fasterxml.jackson.annotation.JsonView;
import net.krotscheck.kangaroo.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.database.jackson.Views;
import net.krotscheck.kangaroo.database.util.SortUtil;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2;
import net.krotscheck.kangaroo.util.PasswordUtil;
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
 * A RESTful API that permits the management of individual user identities.
 *
 * @author Michael Krotscheck
 */
@Path("/identity")
@RolesAllowed({Scope.IDENTITY, Scope.IDENTITY_ADMIN})
@OAuth2
@Transactional
public final class UserIdentityService extends AbstractService {

    /**
     * Search the identities in the system.
     *
     * @param offset      The offset of the first entity to fetch.
     * @param limit       The number of entities to fetch.
     * @param queryString The search term for the query.
     * @param ownerId     An optional user ID to filter by.
     * @param userId      An optional user ID to filter by.
     * @param type        The identity type to search by.
     * @return A list of search results.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView(Views.Public.class)
    @SuppressWarnings({"CPD-START"})
    public Response search(
            @DefaultValue("0") @QueryParam("offset") final Integer offset,
            @DefaultValue("10") @QueryParam("limit") final Integer limit,
            @DefaultValue("") @QueryParam("q") final String queryString,
            @Optional @QueryParam("owner") final UUID ownerId,
            @Optional @QueryParam("user") final UUID userId,
            @Optional @QueryParam("type") final AuthenticatorType type) {

        // Start a query builder...
        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(UserIdentity.class)
                .get();
        BooleanJunction junction = builder.bool();

        Query fuzzy = builder.keyword()
                .fuzzy()
                .onFields(new String[]{"claims", "remoteId"})
                .matching(queryString)
                .createQuery();
        junction = junction.must(fuzzy);

        // Attach an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            Query ownerQuery = builder
                    .keyword()
                    .onField("user.application.owner.id")
                    .matching(owner.getId())
                    .createQuery();
            junction.must(ownerQuery);
        }

        // Attach an user filter.
        User filterByUser = resolveFilterEntity(User.class, userId);
        if (filterByUser != null) {
            Query userQuery = builder
                    .keyword()
                    .onField("user.id")
                    .matching(filterByUser.getId())
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
                        UserIdentity.class);

        return executeQuery(query, offset, limit);
    }

    /**
     * Browse the identities in the system.
     *
     * @param offset  The offset of the first entity to fetch.
     * @param limit   The number of entities to fetch.
     * @param sort    The field on which the entities should be sorted.
     * @param order   The sort order, ASC or DESC.
     * @param ownerId An optional owner ID to filter by.
     * @param userId  An optional user ID to filter by.
     * @param type    An optional authenticator type to filter by.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView(Views.Public.class)
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
            @Optional @QueryParam("user") final UUID userId,
            @Optional @QueryParam("type") final AuthenticatorType type) {

        // Validate the incoming filters.
        User filterByOwner =
                resolveOwnershipFilter(ownerId);
        User filterByUser =
                resolveFilterEntity(User.class, userId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession()
                .createCriteria(UserIdentity.class)
                .createAlias("user", "u")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(UserIdentity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .createAlias("user", "u")
                .addOrder(SortUtil.order(order, sort));

        if (filterByUser != null) {
            browseCriteria
                    .add(Restrictions.eq("u.id", filterByUser.getId()));
            countCriteria
                    .add(Restrictions.eq("u.id", filterByUser.getId()));
        }

        if (type != null) {
            browseCriteria.add(Restrictions.eq("type", type));
            countCriteria.add(Restrictions.eq("type", type));
        }

        if (filterByOwner != null) {
            browseCriteria
                    .createAlias("u.application", "a")
                    .createAlias("a.owner", "o")
                    .add(Restrictions.eq("o.id", filterByOwner.getId()));
            countCriteria
                    .createAlias("u.application", "a")
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
    @JsonView(Views.Public.class)
    public Response getResource(@PathParam("id") final UUID id) {
        UserIdentity identity = getSession().get(UserIdentity.class, id);
        assertCanAccess(identity, getAdminScope());
        return Response.ok(identity).build();
    }

    /**
     * Create a new identity. Note that we only permit the creation of a
     * password identity, on the password authenticator (for owner credentials).
     *
     * @param identity The identity to create.
     * @return A redirect to the location where the identity was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonView(Views.Public.class)
    public Response createResource(final UserIdentity identity) {

        // Input value checks.
        if (identity == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (identity.getId() != null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (identity.getType() == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (identity.getUser() == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (identity.getRemoteId() == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        if (identity.getPassword() == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Resolve the parent identity
        User parent = getSession().get(User.class, identity.getUser().getId());
        if (parent == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Assert that we can create an identity in this application.
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp = parent.getApplication();
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
        }

        // Ensure that the type is 'Password'
        if (!identity.getType().equals(AuthenticatorType.Password)) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Encrypt the password.
        String salt = PasswordUtil.createSalt();
        String passwordHash = PasswordUtil.hash(identity.getPassword(), salt);

        identity.setSalt(salt);
        identity.setPassword(passwordHash);

        // Save it all.
        Session s = getSession();
        s.save(identity);

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(UserIdentityService.class, "getResource")
                .build(identity.getId().toString());

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an identity.
     *
     * @param id       The Unique Identifier for the identity.
     * @param identity The identity to update.
     * @return A response with the identity that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView(Views.Public.class)
    public Response updateResource(@PathParam("id") final UUID id,
                                   final UserIdentity identity) {
        Session s = getSession();

        // Load the old instance.
        UserIdentity current = s.get(UserIdentity.class, id);

        assertCanAccess(current, getAdminScope());

        // Make sure the body ID's match
        if (!current.equals(identity)) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Make sure we're not trying to change the parent entity.
        if (!current.getUser().equals(identity.getUser())) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        // Make sure we're not trying to change the type.
        if (!current.getType().equals(identity.getType())) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }


        // Only permit changing the password if this uses the password
        // authenticator.

        // Transfer all the values we're allowed to edit.
        current.setClaims(identity.getClaims());
        current.setPassword(identity.getPassword());

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
    @JsonView(Views.Public.class)
    public Response deleteResource(@PathParam("id") final UUID id) {
        Session s = getSession();
        UserIdentity identity = s.get(UserIdentity.class, id);

        assertCanAccess(identity, getAdminScope());

        // Let's hope they know what they're doing.
        s.delete(identity);

        return Response.noContent().build();
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.IDENTITY_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.IDENTITY;
    }
}
