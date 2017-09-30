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
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientReferrer;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test the list and filter methods of the ClientReferrer service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ClientReferrerServiceBrowseTest
        extends AbstractSubserviceBrowseTest<Client, ClientReferrer> {

    /**
     * Generic type declaration for list decoding.
     */
    private static final GenericType<ListResponseEntity<ClientReferrer>> LIST_TYPE =
            new GenericType<ListResponseEntity<ClientReferrer>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public ClientReferrerServiceBrowseTest(final ClientType clientType,
                                           final String tokenScope,
                                           final Boolean createUser) {
        super(clientType, tokenScope, createUser);
    }

    /**
     * Test parameters.
     *
     * @return The list of parameters.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT,
                        false
                });
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.CLIENT_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.CLIENT;
    }

    /**
     * Return the list type used to decode browse results.
     *
     * @return The list type.
     */
    @Override
    protected GenericType<ListResponseEntity<ClientReferrer>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the correct parent entity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    @Override
    protected Client getParentEntity(final ApplicationContext context) {
        return context.getClient();
    }

    /**
     * Return the list of entities which should be accessible given a
     * specific token.
     *
     * @param parentEntity The client to filter against.
     * @param token        The oauth token to test against.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<ClientReferrer> getAccessibleEntities(
            final Client parentEntity,
            final OAuthToken token) {
        // If you're an admin, you get to see everything. If you're not, you
        // only get to see what you own.
        OAuthToken attachedToken = getAttached(token);
        if (!attachedToken.getScopes().containsKey(getAdminScope())) {
            return getOwnedEntities(parentEntity, attachedToken);
        }

        // We know you're an admin. Get all applications in the system.
        Criteria criteria = getSession().createCriteria(ClientReferrer.class)
                .add(Restrictions.eq("client", parentEntity));

        // Get all the owned clients.
        return ((List<ClientReferrer>) criteria.list());
    }

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param parentEntity The client to filter against.
     * @param owner        The owner of these entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<ClientReferrer> getOwnedEntities(final Client parentEntity,
                                                    final User owner) {
        // Get all the owned clients.
        return getAttached(owner).getApplications()
                .stream()
                .flatMap(a -> a.getClients().stream())
                .filter(c -> c.equals(parentEntity))
                .flatMap(c -> c.getReferrers().stream())
                .collect(Collectors.toList());
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        String parentId = "";

        Session s = getSession();
        s.getTransaction().begin();
        try {
            ClientReferrer r = s.get(ClientReferrer.class, IdUtil.fromString(id));
            parentId = IdUtil.toString(r.getClient().getId());
        } catch (Exception e) {
            parentId =
                    IdUtil.toString(getParentEntity(getAdminContext()).getId());
        } finally {
            s.getTransaction().commit();
        }

        return getUrlForEntity(parentId, id);
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        String parentId = "";
        String childId = "";

        ClientReferrer referrer = (ClientReferrer) entity;
        if (referrer == null) {
            return getUrlForId(null);
        } else {
            BigInteger referrerId = referrer.getId();
            childId = referrerId == null ? null : IdUtil.toString(referrerId);
        }

        Client client = referrer.getClient();
        if (client == null) {
            return getUrlForId(null);
        } else {
            BigInteger clientId = client.getId();
            parentId = clientId == null ? null : IdUtil.toString(clientId);
        }
        return getUrlForEntity(parentId, childId);
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param parentId The parent ID.
     * @param childId  The Child ID.
     * @return The resource URL.
     */
    private URI getUrlForEntity(final String parentId, final String childId) {
        UriBuilder builder = UriBuilder
                .fromPath("/client")
                .path(parentId)
                .path("referrer");

        if (childId != null) {
            builder.path(childId);
        }

        return builder.build();
    }
}
