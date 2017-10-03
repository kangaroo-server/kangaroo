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
import net.krotscheck.kangaroo.authz.admin.v1.auth.ScopesAllowed;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractClientUri;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientRedirect;
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
 * A RESTful API that permits the management of a client's redirect URI's.
 *
 * @author Michael Krotscheck
 */
@ScopesAllowed({Scope.CLIENT, Scope.CLIENT_ADMIN})
@Transactional
public final class ClientRedirectService extends AbstractService {

    /**
     * The client from which the redirects are extracted.
     */
    private final BigInteger clientId;

    /**
     * Create a new instance of this redirect service.
     *
     * @param clientId The client id, provided by the routed path.
     */
    @Inject
    public ClientRedirectService(
            @PathParam("clientId") final BigInteger clientId) {
        this.clientId = clientId;
    }

    /**
     * Browse the redirects for this client.
     *
     * @param offset The offset of the first scopes to fetch.
     * @param limit  The number of data sets to fetch.
     * @param sort   The field on which the records should be sorted.
     * @param order  The sort order, ASC or DESC.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
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
                .createCriteria(ClientRedirect.class)
                .createAlias("client", "c")
                .add(Restrictions.eq("c.id", client.getId()))
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(ClientRedirect.class)
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
     * Returns a specific redirect.
     *
     * @param id The Unique Identifier for the redirect.
     * @return A response with the redirect that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{32}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(@PathParam("id") final BigInteger id) {
        Session s = getSession();
        Client client = s.get(Client.class, clientId);
        assertCanAccess(client, getAdminScope());

        ClientRedirect redirect = s.get(ClientRedirect.class, id);
        if (redirect == null || !redirect.getClient().equals(client)) {
            throw new NotFoundException();
        }
        assertCanAccess(redirect, getAdminScope());
        return Response.ok(redirect).build();
    }

    /**
     * Create a new redirect for this client.
     *
     * @param redirect The redirect entity to create.
     * @return A redirect to the location where the client was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createResource(final ClientRedirect redirect) {
        Session s = getSession();

        // Make sure we're allowed to access the client.
        Client client = s.get(Client.class, clientId);
        assertCanAccessSubresource(client, getAdminScope());

        // Input value checks.
        if (redirect == null) {
            throw new BadRequestException();
        }
        if (redirect.getId() != null) {
            throw new BadRequestException();
        }
        if (redirect.getUri() == null) {
            throw new BadRequestException();
        }

        // Check for duplicates
        Boolean duplicate = client.getRedirects().stream()
                .map(AbstractClientUri::getUri)
                .anyMatch(uri -> uri.equals(redirect.getUri()));
        if (duplicate) {
            throw new ClientErrorException(Status.CONFLICT);
        }

        // Save it all.
        redirect.setClient(client);
        client.getRedirects().add(redirect);

        s.update(client);
        s.save(redirect);

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(ClientRedirectService.class, "getResource")
                .build(IdUtil.toString(redirect.getId()));

        return Response.created(resourceLocation).build();
    }

    /**
     * Update a redirect for this client.
     *
     * @param id       The Unique Identifier for the redirect.
     * @param redirect The redirect to update.
     * @return A response with the redirect that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{32}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("id") final BigInteger id,
                                   final ClientRedirect redirect) {
        Session s = getSession();

        // Make sure we're allowed to access the client.
        Client client = s.get(Client.class, clientId);
        assertCanAccess(client, getAdminScope());

        // Make sure the old instance exists.
        ClientRedirect currentRedirect = s.get(ClientRedirect.class, id);
        if (currentRedirect == null) {
            throw new NotFoundException();
        }

        // Make sure the parent ID's match
        if (!currentRedirect.getClient().equals(client)) {
            throw new NotFoundException();
        }
        // Make sure the body ID's match
        if (!currentRedirect.equals(redirect)) {
            throw new BadRequestException();
        }
        // Make sure we're not trying to null the redirect.
        if (redirect.getUri() == null) {
            throw new BadRequestException();
        }

        // Make sure we're not creating a duplicate.
        Boolean duplicate = client.getRedirects().stream()
                .filter(r -> !currentRedirect.equals(r))
                .anyMatch(r -> r.getUri().equals(redirect.getUri()));
        if (duplicate) {
            throw new ClientErrorException(Status.CONFLICT);
        }

        // Transfer all the values we're allowed to edit.
        currentRedirect.setUri(redirect.getUri());

        s.update(currentRedirect);

        return Response.ok(redirect).build();
    }

    /**
     * Delete a redirect from a client.
     *
     * @param redirectId The Unique Identifier for the redirect.
     * @return A response that indicates the success of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{32}}")
    public Response deleteResource(
            @PathParam("id") final BigInteger redirectId) {
        Session s = getSession();

        // Make sure we're allowed to access the client.
        Client client = s.get(Client.class, clientId);
        assertCanAccess(client, getAdminScope());

        // Hydrate the redirect
        ClientRedirect redirect = s.get(ClientRedirect.class, redirectId);
        if (redirect == null) {
            throw new NotFoundException();
        }
        // Make sure the parent ID's match
        if (!redirect.getClient().equals(client)) {
            throw new NotFoundException();
        }

        // If we're in the admin app, we can't modify anything.
        if (getAdminApplication().equals(client.getApplication())) {
            throw new ForbiddenException();
        }

        // Execute the command.
        client.getRedirects().remove(redirect);
        s.delete(redirect);
        s.update(client);

        return Response.noContent().build();
    }

    /**
     * Return the redirect required to access ALL resources on this services.
     *
     * @return A string naming the redirect.
     */
    @Override
    protected String getAdminScope() {
        return Scope.CLIENT_ADMIN;
    }

    /**
     * Return the redirect required to access resources on this service.
     *
     * @return A string naming the redirect.
     */
    @Override
    protected String getAccessScope() {
        return Scope.CLIENT;
    }
}
