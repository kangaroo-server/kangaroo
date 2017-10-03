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

package net.krotscheck.kangaroo.authz.admin.v1.servlet;

import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.common.util.PasswordUtil;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.hibernate.migration.DatabaseMigrationState;
import org.apache.commons.configuration.Configuration;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This container lifecycle listener runs once, and only once, when the
 * application has been first installed. It ensures that the admin servlet
 * exists within the database, including its clients, an admin user, and all
 * the necessary scopes.
 *
 * @author Michael Krotscheck
 */
public final class FirstRunContainerLifecycleListener
        implements ContainerLifecycleListener {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(FirstRunContainerLifecycleListener.class);

    /**
     * The session factory for this instance.
     */
    private final SessionFactory sessionFactory;

    /**
     * Servlet configuration.
     */
    private final Configuration servletConfig;

    /**
     * The migration state. Currently injected in order to ensure an
     * order-of-operations: The migration must happen before we build the
     * first application data.
     */
    @SuppressWarnings("PMD")
    private final DatabaseMigrationState migrationState;

    /**
     * Create a new instance of this factory.
     *
     * @param sessionFactory The session factory.
     * @param servletConfig  The servlet's database-persisted configuration.
     * @param migrationState The database migration state.
     */
    @Inject
    public FirstRunContainerLifecycleListener(
            final SessionFactory sessionFactory,
            @Named(ServletConfigFactory.GROUP_NAME)
            final Configuration servletConfig,
            final DatabaseMigrationState migrationState) {
        this.sessionFactory = sessionFactory;
        this.servletConfig = servletConfig;
        this.migrationState = migrationState;
    }

    /**
     * Bootstrap the application, and return it.
     *
     * @return Kangaroo's admin application instance.
     */
    private Application bootstrapApplication() {
        logger.debug("Bootstrapping Application");

        Session s = sessionFactory.openSession();

        // Create the application.
        Application servletApp = new Application();
        servletApp.setName("Kangaroo");

        // Create the application's client.
        Client servletClient = new Client();
        servletClient.setApplication(servletApp);
        servletClient.setName("Kangaroo Web UI");
        servletClient.setType(ClientType.OwnerCredentials);

        // Create the password authenticator
        Authenticator passwordAuth = new Authenticator();
        passwordAuth.setType(AuthenticatorType.Password);
        passwordAuth.setClient(servletClient);

        // Create the scopes
        SortedMap<String, ApplicationScope> userScopes = new TreeMap<>();
        for (String scopeName : Scope.userScopes()) {
            ApplicationScope newScope = new ApplicationScope();
            newScope.setApplication(servletApp);
            newScope.setName(scopeName);
            userScopes.put(scopeName, newScope);
        }
        SortedMap<String, ApplicationScope> adminScopes = new TreeMap<>();
        for (String scopeName : Scope.adminScopes()) {
            ApplicationScope newScope = new ApplicationScope();
            newScope.setApplication(servletApp);
            newScope.setName(scopeName);
            adminScopes.put(scopeName, newScope);
        }

        // Create the roles.
        Role adminRole = new Role();
        adminRole.setName("admin");
        adminRole.setApplication(servletApp);
        adminRole.setScopes(adminScopes);

        Role memberRole = new Role();
        memberRole.setName("member");
        memberRole.setApplication(servletApp);
        memberRole.setScopes(userScopes);
        servletApp.setDefaultRole(memberRole);

        // Create the first admin
        User adminUser = new User();
        adminUser.setApplication(servletApp);
        adminUser.setRole(adminRole);

        // Ensure the new user is the owner of the admin app
        servletApp.setOwner(adminUser);

        // Create the admin's login identity.
        UserIdentity adminIdentity = new UserIdentity();
        adminIdentity.setType(AuthenticatorType.Password);
        adminIdentity.setRemoteId("admin");
        adminIdentity.setUser(adminUser);
        adminIdentity.setSalt(PasswordUtil.createSalt());
        adminIdentity.setPassword(
                PasswordUtil.hash("admin", adminIdentity.getSalt()));

        s.getTransaction().begin();
        s.save(servletApp);
        s.save(servletClient);
        s.save(passwordAuth);
        adminScopes.forEach(s::save);
        userScopes.forEach(s::save);
        s.save(adminRole);
        s.save(memberRole);
        s.save(adminUser);
        s.save(adminIdentity);
        s.getTransaction().commit();

        logger.debug(String.format("Application ID: %s",
                IdUtil.toString(servletApp.getId())));
        logger.debug(String.format("Admin User ID: %s",
                IdUtil.toString(adminUser.getId())));
        logger.debug(String.format("WebUI Client ID: %s",
                IdUtil.toString(servletClient.getId())));
        logger.debug("Application created. Let's rock!");

        servletConfig.addProperty(Config.APPLICATION_ID,
                IdUtil.toString(servletApp.getId()));
        servletConfig.addProperty(Config.APPLICATION_CLIENT_ID,
                IdUtil.toString(servletClient.getId()));
        servletConfig.addProperty(Config.APPLICATION_ADMIN_ID,
                IdUtil.toString(adminUser.getId()));

        // Refresh the servlet app, populating all persisted references.
        s.refresh(servletApp);
        s.close();

        return servletApp;
    }

    /**
     * Invoked at the {@link Container container} start-up. This method is
     * invoked even when application is reloaded and new instance of
     * application has started.
     *
     * @param container container that has been started.
     */
    @Override
    public void onStartup(final Container container) {
        Boolean firstRun = servletConfig.getBoolean(Config.FIRST_RUN, false);
        if (!firstRun) {
            bootstrapApplication();
            servletConfig.addProperty(Config.FIRST_RUN, true);
        }
    }

    /**
     * Invoked when the {@link Container container} has been reloaded.
     *
     * @param container container that has been reloaded.
     */
    @Override
    public void onReload(final Container container) {
    }

    /**
     * Invoke at the {@link Container container} shut-down. This method is
     * invoked even before
     * the application is being stopped as a part of reload.
     *
     * @param container container that has been shut down.
     */
    @Override
    public void onShutdown(final Container container) {
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(FirstRunContainerLifecycleListener.class)
                    .to(ContainerLifecycleListener.class)
                    .in(Singleton.class);
        }
    }
}
