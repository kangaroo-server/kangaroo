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

import net.krotscheck.kangaroo.common.exception.exception.HttpNotFoundException;
import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.ServletConfigFactory;
import org.apache.commons.configuration.Configuration;
import org.glassfish.hk2.api.ServiceLocator;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.QueryBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

/**
 * Abstract implementation of our common services.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractService {

    /**
     * Service Locator.
     */
    @Inject
    private ServiceLocator serviceLocator;

    /**
     * Servlet Configuration.
     */
    @Inject
    @Named(ServletConfigFactory.GROUP_NAME)
    private Configuration config;

    /**
     * Hibernate session.
     */
    @Inject
    private Session session;

    /**
     * The hibernate search factory.
     */
    @Inject
    private SearchFactory searchFactory;

    /**
     * The hibernate fulltext session.
     */
    @Inject
    private FullTextSession fullTextSession;

    /**
     * The Security context.
     */
    @Inject
    private SecurityContext securityContext;

    /**
     * The URI info for this request.
     */
    @Inject
    private UriInfo uriInfo;

    /**
     * Retrieve the service locator for this context.
     *
     * @return The locator.
     */
    public final ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    /**
     * Set (via injection, usually) the service locator for this resource.
     *
     * @param serviceLocator The locator.
     */
    public final void setServiceLocator(final ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    /**
     * Get the URI info for the current request.
     *
     * @return The URIInfo instance.
     */
    public final UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * Set the URI info instance for the current request.
     *
     * @param uriInfo The new URIInfo instance.
     */
    public final void setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    /**
     * Get the session.
     *
     * @return The injected session.
     */
    public final Session getSession() {
        return session;
    }

    /**
     * Set the session.
     *
     * @param session The session.
     */
    public final void setSession(final Session session) {
        this.session = session;
    }

    /**
     * Retrieve the search factory.
     *
     * @return The injected search factory.
     */
    public final SearchFactory getSearchFactory() {
        return searchFactory;
    }

    /**
     * Set an instance of the search factory.
     *
     * @param searchFactory The search factory.
     */
    public final void setSearchFactory(final SearchFactory searchFactory) {
        this.searchFactory = searchFactory;
    }

    /**
     * Get the full text search session.
     *
     * @return The injected lucene session.
     */
    public final FullTextSession getFullTextSession() {
        return fullTextSession;
    }

    /**
     * Set the full text search session.
     *
     * @param fullTextSession The FTSession.
     */
    public final void setFullTextSession(
            final FullTextSession fullTextSession) {
        this.fullTextSession = fullTextSession;
    }

    /**
     * Get the configuration.
     *
     * @return A configuration!
     */
    public final Configuration getConfig() {
        return config;
    }

    /**
     * Set the request configuration.
     *
     * @param config A request configuration.
     */
    public final void setConfig(final Configuration config) {
        this.config = config;
    }

    /**
     * Get the security context.
     *
     * @return The request security context.
     */
    public final SecurityContext getSecurityContext() {
        return securityContext;
    }

    /**
     * Set a new security context.
     *
     * @param securityContext A new security context.
     */
    public final void setSecurityContext(
            final SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    /**
     * Retrieve the current user for this request.
     *
     * @return The current user, or null if not set.
     */
    protected final User getCurrentUser() {
        OAuthToken t = (OAuthToken) securityContext.getUserPrincipal();
        if (t.getIdentity() != null) {
            return t.getIdentity().getUser();
        }
        return null;
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    protected abstract String getAdminScope();

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    protected abstract String getAccessScope();

    /**
     * Retrieve the admin application database entry for the current running
     * test.
     *
     * @return The admin application, assuming it's been bootstrapped.
     */
    protected final Application getAdminApplication() {
        Session s = getSession();
        String configId = config.getString(Config.APPLICATION_ID);
        UUID appId = UUID.fromString(configId);
        return s.get(Application.class, appId);
    }

    /**
     * This method tests whether a particular entity may be accessed. This is
     * the case if one of two conditions is true. The first is simple- if the
     * user has the administration scope of ENTITYNAME_ADMIN, then they may
     * access this entity. If they only have the ENTITYNAME scope, then they
     * must also be the owner of this entity (the owner of the application to
     * which this entity belongs.
     *
     * @param entity        The entity to check.
     * @param requiredScope The scope required to access this
     *                      entity.
     */
    protected final void assertCanAccess(final AbstractEntity entity,
                                         final String requiredScope) {
        // No null entities permitted.
        if (entity == null) {
            throw new HttpNotFoundException();
        }

        // Are we the entity owner?
        User u = getCurrentUser();
        if (u != null && u.equals(entity.getOwner())) {
            return;
        }

        // If we don't have the scope.
        if (getSecurityContext().isUserInRole(requiredScope)) {
            return;
        }

        // Not permitted, exit.
        throw new HttpNotFoundException();
    }

    /**
     * This method tests whether a particular subresource entity may be
     * accessed. It defers most of its logic to assertCanAccess, except that
     * it will rethrow HttpNotFoundExceptions as BadRequestExceptions in the
     * case where a user does not have permissions to see the parent resource.
     *
     * @param entity              The entity to check.
     * @param requiredParentScope The scope required to access the parent
     *                            entity.
     */
    protected final void assertCanAccessSubresource(
            final AbstractEntity entity, final String requiredParentScope) {
        // No null entities permitted.
        if (entity == null) {
            throw new HttpNotFoundException();
        }

        try {
            assertCanAccess(entity, requiredParentScope);
        } catch (HttpNotFoundException e) {
            throw new BadRequestException();
        }
    }

    /**
     * Determine the appropriate owner on which we should be filtering.
     *
     * @param ownerId The passed-in owner id, could be null.
     * @return A , or null, indicating a valid filter action.
     */
    protected final User resolveOwnershipFilter(final UUID ownerId) {

        String adminScope = getAdminScope();

        if (getSecurityContext().isUserInRole(adminScope)) {
            if (ownerId == null) {
                return null;
            }
            User owner = getSession().get(User.class, ownerId);
            if (owner == null) {
                throw new HttpStatusException(Status.BAD_REQUEST);
            }
            return owner;
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            // This will catch ClientCredentials clients.
            throw new Rfc6749Exception.InvalidScopeException();
        }

        if (ownerId != null && !currentUser.getId().equals(ownerId)) {
            throw new Rfc6749Exception.InvalidScopeException();
        }

        return currentUser;
    }

    /**
     * Attempt to resolve the provided ID into the requested entity type.
     *
     * @param roleClass The entity type to resolve.
     * @param entityId  The ID to resolve.
     * @param <T>       The return type.
     * @return The resolved entity, or an exception if it cannot be accessed.
     */
    protected final <T extends AbstractEntity> T resolveFilterEntity(
            final Class<T> roleClass,
            final UUID entityId) {

        String ownerScope = getAccessScope();
        String adminScope = getAdminScope();

        // Resolve the entity. This will error if an entity could not be
        // resolved.
        T entity = resolveEntityInput(roleClass, entityId);

        // If we're an admin, just return what we have so far.
        if (getSecurityContext().isUserInRole(adminScope)) {
            return entity;
        }

        // If we're not scoped, throw an exception
        if (!getSecurityContext().isUserInRole(ownerScope)) {
            throw new InvalidScopeException();
        }

        // If we don't have a current user, exit. This means that we have a
        // client-credentials client with no admin scope in the token.
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new InvalidScopeException();
        }

        // If we didn't ask for anything, return the default value.
        if (entityId == null) {
            return null;
        }

        // If we don't own the entity, bad request. This is identical to the
        // above, as we cannot expose to an improperly scoped token that
        // an entity might exist.
        if (!currentUser.equals(entity.getOwner())) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }

        return entity;
    }

    /**
     * Build a search query.
     *
     * @param entityType  The entity type to use.
     * @param queryFields The lucene index fields to query on.
     * @param query       The query string.
     * @param <T>         The type of entity to query.
     * @return The constructed full text query for this entity type.
     */
    protected final <T extends AbstractEntity> FullTextQuery buildQuery(
            final Class<T> entityType,
            final String[] queryFields,
            final String query) {

        // Start a query builder...
        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(entityType)
                .get();

        org.apache.lucene.search.Query luceneQuery = builder
                .keyword()
                .fuzzy()
                .onFields(queryFields)
                .ignoreFieldBridge()
                .matching(query)
                .createQuery();

        return getFullTextSession()
                .createFullTextQuery(luceneQuery, entityType);
    }

    /**
     * Execute a search query.
     *
     * @param query  The fulltext query to execute.
     * @param offset The query offset.
     * @param limit  The query limit.
     * @return A response with the return values of this type.
     */
    protected final Response executeQuery(
            final FullTextQuery query,
            final int offset,
            final int limit) {
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        return ListResponseBuilder
                .builder()
                .offset(offset)
                .limit(limit)
                .addResult(query.list())
                .total(query.getResultSize())
                .build();
    }

    /**
     * Provided a type and an entity, attempts to resolve a fresh entity from
     * the database session.
     *
     * @param requestedType The type to resolve.
     * @param entity        The entity.
     * @param <K>           The type to return (same as type to resolve).
     * @return Null if provided entity is null, otherwise entity.
     * @throws HttpStatusException When the entity is malformed in some way.
     */
    protected final <K extends AbstractEntity> K resolveEntityInput(
            final Class<K> requestedType,
            final K entity) {

        // Null value check.
        if (entity == null) {
            return null;
        }

        return resolveEntityInput(requestedType, entity.getId());
    }


    /**
     * Provided a type and an id, attempts to resolve a fresh entity from
     * the database session.
     *
     * @param requestedType The type to resolve.
     * @param entityId      The entity id.
     * @param <K>           The type to return (same as type to resolve).
     * @return Null if provided entity is null, otherwise entity.
     * @throws HttpStatusException When the id is malformed in some way.
     */
    protected final <K extends AbstractEntity> K resolveEntityInput(
            final Class<K> requestedType,
            final UUID entityId) {
        // Sanity check
        if (entityId == null) {
            return null;
        }

        // Attempt to resolve...
        K entity = getSession().get(requestedType, entityId);
        if (entity == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        return entity;
    }

    /**
     * Provided a type and an entity, requires the existence of this entity.
     *
     * @param requestedType The type to resolve.
     * @param entity        The entity.
     * @param <K>           The type to return (same as type to resolve).
     * @return Null if provided entity is null, otherwise entity.
     * @throws HttpStatusException When the entity is malformed in some way.
     */
    protected final <K extends AbstractEntity> K requireEntityInput(
            final Class<K> requestedType,
            final K entity) {
        K resolved = resolveEntityInput(requestedType, entity);
        if (resolved == null) {
            throw new HttpStatusException(Status.BAD_REQUEST);
        }
        return resolved;
    }
}
