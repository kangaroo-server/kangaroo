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

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.util.PasswordUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

/**
 * This class assists in the creation of a test environment, by bootstrapping
 * applications, clients, their enabled authenticators and flows, as well as
 * other miscellaneous components.
 * <p>
 * Note that this class is a bit volatile, as it makes the implicit
 * assumption that all the resources it needs will be created before they're
 * used. In other words, if you've got weird issues, then you're probably
 * using this class wrong.
 *
 * @author Michael Krotscheck
 */
public final class EnvironmentBuilder {

    /**
     * Static timezone.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * The builder's hibernate session.
     */
    private Session session;

    /**
     * The current application context.
     */
    private Application application;

    /**
     * The most recent created scope.
     */
    private ApplicationScope scope;

    /**
     * The current application scopes.
     */
    private SortedMap<String, ApplicationScope> scopes = new TreeMap<>();

    /**
     * Get the list of tracked entities.
     *
     * @return The current list of tracked entities.
     */
    public List<AbstractEntity> getTrackedEntities() {
        return Collections.unmodifiableList(trackedEntities);
    }

    /**
     * Return the current list of active scopes.
     *
     * @return The list of scopes.
     */
    public SortedMap<String, ApplicationScope> getScopes() {
        return Collections.unmodifiableSortedMap(scopes);
    }

    /**
     * The current role context.
     */
    private Role role;

    /**
     * The current client context.
     */
    private Client client;

    /**
     * The current authenticator context.
     */
    private Authenticator authenticator;

    /**
     * The user context.
     */
    private User user;

    /**
     * The user identity context.
     */
    private UserIdentity userIdentity;

    /**
     * The oauth token context.
     */
    private OAuthToken token;

    /**
     * Get the current application.
     *
     * @return The current application.
     */
    public Application getApplication() {
        return application;
    }

    /**
     * Get the current role.
     *
     * @return The current role.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Get the current client.
     *
     * @return The current client.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Get the current authenticator.
     *
     * @return The current authenticator.
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * Get the current user.
     *
     * @return The current user.
     */
    public User getUser() {
        return user;
    }

    /**
     * Get the current user identity.
     *
     * @return The current user identity.
     */
    public UserIdentity getUserIdentity() {
        return userIdentity;
    }

    /**
     * Get the current token.
     *
     * @return The current token.
     */
    public OAuthToken getToken() {
        return token;
    }

    /**
     * Get the current scope.
     *
     * @return The current scope.
     */
    public ApplicationScope getScope() {
        return scope;
    }

    /**
     * The list of entities that are under management by this builder.
     */
    private final List<AbstractEntity> trackedEntities = new ArrayList<>();

    /**
     * Create a new builder.
     *
     * @param session A Hibernate session to use.
     * @param name    The name of the application.
     */
    public EnvironmentBuilder(final Session session, final String name) {
        this.session = session;

        application = new Application();
        application.setName(name);

        persist(application);
    }

    /**
     * Create a new builder around an existing application. In this case,
     * only the application's ID is used, as the entity may be a member of a
     * different session. Note that this will try to populate some of the
     * internal fields with what's already been loaded into the database, so
     * missing fields may cause errors.
     *
     * @param session A Hibernate session to use.
     * @param app     The Application entity to wrap.
     */
    public EnvironmentBuilder(final Session session,
                              final Application app) {
        this.session = session;

        // Load this entity from the provided session.
        this.application = session.get(Application.class, app.getId());
        this.scopes.putAll(this.application.getScopes());
        this.client = this.application.getClients().get(0);
        this.authenticator = this.client.getAuthenticators().get(0);
        this.user = this.application.getUsers().get(0);
        if (this.application.getRoles().size() > 0) {
            this.role = this.application.getRoles().get(0);
        }

        session.clear();
    }

    /**
     * Create a new builder.
     *
     * @param session A Hibernate session to use.
     */
    public EnvironmentBuilder(final Session session) {
        this(session, UUID.randomUUID().toString());
    }

    /**
     * Add a role to this application.
     *
     * @param name The name of the role.
     * @return This environment builder.
     */
    public EnvironmentBuilder role(final String name) {
        role = new Role();
        role.setApplication(application);
        role.setName(name);
        persist(role);
        return this;
    }

    /**
     * Add a scope to this application.
     *
     * @param name The name of the scope.
     * @return This environment builder.
     */
    public EnvironmentBuilder scope(final String name) {
        scope = new ApplicationScope();
        scope.setName(name);
        scope.setApplication(application);
        scopes.put(name, scope);
        persist(scope);
        return this;
    }

    /**
     * Add a list of scopes to this application.
     *
     * @param scopes The list of scopes to add.
     * @return This builder.
     */
    public EnvironmentBuilder scopes(final List<String> scopes) {
        for (String scope : scopes) {
            scope(scope);
        }
        return this;
    }

    /**
     * Add a client to this application.
     *
     * @param type The client type.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type) {
        return client(type, false);
    }

    /**
     * Add a client to this application.
     *
     * @param type The client type.
     * @param name An explicit client name.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type,
                                     final String name) {
        return client(type, name, false);
    }

    /**
     * Add a client, with a secret, to this application.
     *
     * @param isPrivate Is this a private client or not?
     * @param type      The client type.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type,
                                     final Boolean isPrivate) {
        return client(type, "Test Client", isPrivate);
    }

    /**
     * Add a client, with a name, to this application.
     *
     * @param isPrivate Is this a private client or not?
     * @param name      An explicit client name.
     * @param type      The client type.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type,
                                     final String name,
                                     final Boolean isPrivate) {
        client = new Client();
        client.setName(name);
        client.setType(type);
        client.setApplication(application);

        if (isPrivate) {
            client.setClientSecret(UUID.randomUUID().toString());
        }

        persist(client);

        return this;
    }

    /**
     * Add a redirect to the current client context.
     *
     * @param redirect The Redirect URI for the client.
     * @return This builder.
     */
    public EnvironmentBuilder redirect(final String redirect) {
        if (client.getRedirects() == null) {
            client.setRedirects(new HashSet<>());
        }

        URI redirectUri = UriBuilder.fromUri(redirect).build();
        client.getRedirects().add(redirectUri);

        persist(client);

        return this;
    }

    /**
     * Add a referrer to the current client context.
     *
     * @param referrer The Referral URI for the client.
     * @return This builder.
     */
    public EnvironmentBuilder referrer(final String referrer) {
        if (client.getReferrers() == null) {
            client.setReferrers(new HashSet<>());
        }

        URI referrerUri = UriBuilder.fromUri(referrer).build();
        client.getReferrers().add(referrerUri);

        persist(client);

        return this;
    }

    /**
     * Enable an authenticator for the current client context.
     *
     * @param name The authenticator to enable.
     * @return This builder.
     */
    public EnvironmentBuilder authenticator(final String name) {
        authenticator = new Authenticator();
        authenticator.setClient(client);
        authenticator.setType(name);
        persist(authenticator);
        return this;
    }

    /**
     * Create a new user for this application.
     *
     * @return This builder.
     */
    public EnvironmentBuilder user() {
        user = new User();
        user.setApplication(client.getApplication());
        user.setRole(role);
        persist(user);

        return this;
    }

    /**
     * Persist and track an entity.
     *
     * @param e The entity to persist.
     */
    private void persist(final AbstractEntity e) {

        // Set created/updated dates for all entities.
        e.setCreatedDate(Calendar.getInstance(UTC));
        e.setModifiedDate(Calendar.getInstance(UTC));

        Transaction t = session.beginTransaction();
        session.saveOrUpdate(e);
        t.commit();

        // Evict the entity, so that it's freshly loaded later.
        session.evict(e);

        if (!trackedEntities.contains(e)) {
            trackedEntities.add(e);
        }

        // Persist all changes.
        session.flush();

        // Make sure everything's evicted, so we can load it cleanly later.
        session.clear();
    }

    /**
     * Add a login for the current user context.
     *
     * @param login    The user login.
     * @param password The user password.
     * @return This builder.
     * @throws Exception Thrown if the security algorithm isn't present.
     */
    public EnvironmentBuilder login(final String login, final String password) {
        userIdentity = new UserIdentity();
        userIdentity.setUser(user);
        userIdentity.setRemoteId(login);
        userIdentity.setSalt(PasswordUtil.createSalt());
        userIdentity.setPassword(PasswordUtil.hash(password,
                userIdentity.getSalt()));
        userIdentity.setAuthenticator(authenticator);
        persist(userIdentity);
        return this;
    }

    /**
     * Add a unique identity to the current user context.
     *
     * @param remoteIdentity The unique identity.
     * @return This builder.
     */
    public EnvironmentBuilder identity(final String remoteIdentity) {
        userIdentity = new UserIdentity();
        userIdentity.setUser(user);
        userIdentity.setRemoteId(remoteIdentity);
        userIdentity.setAuthenticator(authenticator);
        persist(userIdentity);
        return this;
    }

    /**
     * Add an authorization code to the current client/redirect scope.
     *
     * @return This builder.
     */
    public EnvironmentBuilder authToken() {
        String redirect =
                getClient().getRedirects().iterator().next().toString();

        return token(OAuthTokenType.Authorization, false, null, redirect, null);
    }

    /**
     * Add a bearer token to this user.
     *
     * @return This builder.
     */
    public EnvironmentBuilder bearerToken() {
        return bearerToken(null);
    }

    /**
     * Add a scoped bearer token to this user.
     *
     * @param scopes The scopes to assign to this token.
     * @return This builder.
     */
    public EnvironmentBuilder bearerToken(final String scopes) {
        return token(OAuthTokenType.Bearer, false, scopes, null, null);
    }

    /**
     * Add a refresh token.
     *
     * @return This builder.
     */
    public EnvironmentBuilder refreshToken() {
        return token(OAuthTokenType.Refresh, false, null, null, token);
    }

    /**
     * Customize a token.
     *
     * @param type        The token type.
     * @param expired     Whether it's expired.
     * @param scopeString The requested scope.
     * @param redirect    The redirect URL.
     * @param authToken   An optional auth token.
     * @return This builder.
     */
    public EnvironmentBuilder token(final OAuthTokenType type,
                                    final Boolean expired,
                                    final String scopeString,
                                    final String redirect,
                                    final OAuthToken authToken) {
        token = new OAuthToken();
        token.setTokenType(type);
        token.setClient(client);
        token.setIdentity(userIdentity);
        token.setAuthToken(authToken);

        if (!StringUtils.isEmpty(redirect)) {
            URI redirectUri = UriBuilder.fromUri(redirect).build();
            token.setRedirect(redirectUri);
        }

        // If expired, else use defaults.
        if (expired) {
            token.setExpiresIn(-100);
        } else {
            token.setExpiresIn(100);
        }

        // Split and attach the scopes.
        SortedMap<String, ApplicationScope> newScopes = new TreeMap<>();
        if (!StringUtils.isEmpty(scopeString)) {
            for (String scope : scopeString.split(" ")) {
                newScopes.put(scope, scopes.get(scope));
            }
        }
        token.setScopes(newScopes);

        persist(token);
        return this;
    }

    /**
     * Add an identity claim.
     *
     * @param name  Name of the field.
     * @param value Value of the field.
     * @return This builder.
     */
    public EnvironmentBuilder claim(final String name, final String value) {
        // User identity has to be separately hydrated and persisted, in
        // order to make sure we get the latest entity.
        session.refresh(userIdentity);
        userIdentity.getClaims().putIfAbsent(name, value);
        persist(userIdentity);
        return this;
    }

    /**
     * Set the owner for the current application.
     *
     * @param user The new owner.
     * @return This builder.
     */
    public EnvironmentBuilder owner(final User user) {
        // Reload the owner from the current session.
        User sessionUser = session.get(User.class, user.getId());
        application.setOwner(sessionUser);
        persist(application);
        return this;
    }

    /**
     * Get the owner of this app.
     *
     * @return The application owner.
     */
    public User getOwner() {
        return application.getOwner();
    }

    /**
     * Clear all created entities from the database.
     */
    public void clear() {
        Transaction t = session.beginTransaction();
        for (int i = trackedEntities.size() - 1; i >= 0; i--) {
            AbstractEntity e = trackedEntities.get(i);

            e = session.get(e.getClass(), e.getId());
            if (e != null) {
                session.delete(e);
            }
        }
        t.commit();
        trackedEntities.clear();

        application = null;
        scopes.clear();
        scope = null;
        role = null;
        client = null;
        authenticator = null;
        user = null;
        userIdentity = null;
        token = null;
    }
}
