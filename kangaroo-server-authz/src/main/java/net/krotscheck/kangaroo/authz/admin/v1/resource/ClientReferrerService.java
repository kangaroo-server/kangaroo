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
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractClientUri;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientReferrer;
import net.krotscheck.kangaroo.authz.common.database.util.SortUtil;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
 * A RESTful API that permits the management of a client's referrer URI's.
 *
 * @author Michael Krotscheck
 */
@ScopesAllowed({Scope.CLIENT, Scope.CLIENT_ADMIN})
@Transactional
@Api(tags = "Client",
        authorizations = {
                @Authorization(value = "Kangaroo", scopes = {
                        @AuthorizationScope(
                                scope = Scope.CLIENT,
                                description = "Modify referrers in"
                                        + " one application."),
                        @AuthorizationScope(
                                scope = Scope.CLIENT_ADMIN,
                                description = "Modify referrers in"
                                        + " all applications.")
                })
        })
public final class ClientReferrerService extends AbstractService {

    /**
     * The client from which the referrers are extracted.
     */
    private final BigInteger clientId;

    /**
     * Create a new instance of this referrer service.
     *
     * @param clientId The client id, provided by the routed path.
     */
    @Inject
    public ClientReferrerService(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("clientId") final BigInteger clientId) {
        this.clientId = clientId;
    }

    /**
     * Browse the referrers for this client.
     *
     * @param offset The offset of the first scopes to fetch.
     * @param limit  The number of data sets to fetch.
     * @param sort   The field on which the records should be sorted.
     * @param order  The sort order, ASC or DESC.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Browse client referrers")
    @SuppressWarnings("CPD-START")
    public Response browse(
            @QueryParam(ApiParam.OFFSET_QUERY)
            @DefaultValue(ApiParam.OFFSET_DEFAULT) final int offset,
            @QueryParam(ApiParam.LIMIT_QUERY)
            @DefaultValue(ApiParam.LIMIT_DEFAULT) final int limit,
            @QueryParam(ApiParam.SORT_QUERY)
            @DefaultValue(ApiParam.SORT_DEFAULT) final String sort,
            @QueryParam(ApiParam.ORDER_QUERY)
            @DefaultValue(ApiParam.ORDER_DEFAULT) final SortOrder order) {
        Session s = getSession();

        // Make sure we can read the client.
        Client client = s.get(Client.class, clientId);
        assertCanAccess(client, getAdminScope());

        // Build a count criteria
        Criteria countCriteria = getSession()
                .createCriteria(ClientReferrer.class)
                .createAlias("client", "c")
                .add(Restrictions.eq("c.id", client.getId()))
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(ClientReferrer.class)
                .createAlias("client", "c")
                .add(Restrictions.eq("c.id", client.getId()))
                .setFirstResult(offset)
                .setMaxResults(limit)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .addOrder(SortUtil.order(order, sort));

        // Always filter by client
        browseCriteria.add(Restrictions.eq("c.id", client.getId()));
        countCriteria.add(Restrictions.eq("c.id", client.getId()));

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
     * Returns a specific referrer.
     *
     * @param id The Unique Identifier for the referrer.
     * @return A response with the referrer that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{32}}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read client referrer")
    public Response getResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id) {
        Session s = getSession();
        Client client = s.get(Client.class, clientId);
        assertCanAccess(client, getAdminScope());

        ClientReferrer referrer = s.get(ClientReferrer.class, id);
        // Make sure the parent ID's match
        if (referrer == null || !referrer.getClient().equals(client)) {
            throw new NotFoundException();
        }
        assertCanAccess(referrer, getAdminScope());
        return Response.ok(referrer).build();
    }


    /**
     * Create a new referrer for this client.
     *
     * @param referrer The referrer entity to create.
     * @return A referrer to the location where the client was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create client referrer")
    public Response createResource(final ClientReferrer referrer) {
        Session s = getSession();

        // Make sure we're allowed to access the client.
        Client client = s.get(Client.class, clientId);
        assertCanAccessSubresource(client, getAdminScope());

        // Input value checks.
        if (referrer == null) {
            throw new BadRequestException();
        }
        if (referrer.getId() != null) {
            throw new BadRequestException();
        }
        if (referrer.getUri() == null) {
            throw new BadRequestException();
        }

        // Check for duplicates
        Boolean duplicate = client.getReferrers().stream()
                .map(AbstractClientUri::getUri)
                .anyMatch(uri -> uri.equals(referrer.getUri()));
        if (duplicate) {
            throw new ClientErrorException(Status.CONFLICT);
        }

        // Save it all.
        referrer.setClient(client);
        client.getReferrers().add(referrer);

        s.update(client);
        s.save(referrer);

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(ClientReferrerService.class, "getResource")
                .build(IdUtil.toString(referrer.getId()));

        return Response.created(resourceLocation).build();
    }

    /**
     * Update a referrer for this client.
     *
     * @param id       The Unique Identifier for the referrer.
     * @param referrer The referrer to update.
     * @return A response with the referrer that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{32}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update client referrer")
    public Response updateResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id,
            final ClientReferrer referrer) {
        Session s = getSession();

        // Make sure we're allowed to access the client.
        Client client = s.get(Client.class, clientId);
        assertCanAccess(client, getAdminScope());

        // Make sure the old instance exists.
        ClientReferrer currentReferrer = s.get(ClientReferrer.class, id);
        if (currentReferrer == null) {
            throw new NotFoundException();
        }
        // Make sure the parent ID's match
        if (!currentReferrer.getClient().equals(client)) {
            throw new NotFoundException();
        }

        // Make sure the body ID's match
        if (!currentReferrer.equals(referrer)) {
            throw new BadRequestException();
        }
        // Make sure we're not trying to null the redirect.
        if (referrer.getUri() == null) {
            throw new BadRequestException();
        }

        // Make sure we're not creating a duplicate.
        Boolean duplicate = client.getReferrers().stream()
                .filter(r -> !currentReferrer.equals(r))
                .anyMatch(r -> r.getUri().equals(referrer.getUri()));
        if (duplicate) {
            throw new ClientErrorException(Status.CONFLICT);
        }

        // Transfer all the values we're allowed to edit.
        currentReferrer.setUri(referrer.getUri());

        s.update(currentReferrer);

        return Response.ok(referrer).build();
    }


    /**
     * Delete a referrer from a client.
     *
     * @param referrerId The Unique Identifier for the referrer.
     * @return A response that indicates the success of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{32}}")
    @ApiOperation(value = "Delete client referrer")
    public Response deleteResource(
            @PathParam("id") final BigInteger referrerId) {
        Session s = getSession();

        // Make sure we're allowed to access the client.
        Client client = s.get(Client.class, clientId);
        assertCanAccess(client, getAdminScope());

        // Hydrate the referrer
        ClientReferrer referrer = s.get(ClientReferrer.class, referrerId);
        if (referrer == null) {
            throw new NotFoundException();
        }
        // Make sure the parent ID's match
        if (!referrer.getClient().equals(client)) {
            throw new NotFoundException();
        }

        // If we're in the admin app, we can't modify anything.
        if (getAdminApplication().equals(client.getApplication())) {
            throw new ForbiddenException();
        }

        // Execute the command.
        client.getReferrers().remove(referrer);
        s.delete(referrer);
        s.update(client);

        return Response.noContent().build();
    }

    /**
     * Return the referrer required to access ALL resources on this services.
     *
     * @return A string naming the referrer.
     */
    @Override
    protected String getAdminScope() {
        return Scope.CLIENT_ADMIN;
    }

    /**
     * Return the referrer required to access resources on this service.
     *
     * @return A string naming the referrer.
     */
    @Override
    protected String getAccessScope() {
        return Scope.CLIENT;
    }
}
