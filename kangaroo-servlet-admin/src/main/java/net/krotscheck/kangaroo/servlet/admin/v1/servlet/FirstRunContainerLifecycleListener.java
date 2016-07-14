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

package net.krotscheck.kangaroo.servlet.admin.v1.servlet;

import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.util.PasswordUtil;
import org.apache.commons.configuration.Configuration;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
     * Create a new instance of this factory.
     *
     * @param sessionFactory The session factory.
     * @param servletConfig  The servlet's database-persisted configuration.
     */
    @Inject
    public FirstRunContainerLifecycleListener(
            final SessionFactory sessionFactory,
            @Named(ServletConfigFactory.GROUP_NAME)
            final Configuration servletConfig) {
        this.sessionFactory = sessionFactory;
        this.servletConfig = servletConfig;
    }

    /**
     * Bootstrap the application, and return it.
     *
     * @return Kangaroo's admin application instance.
     */
    private Application bootstrapApplication() {
        logger.info("Bootstrapping Application");

        Session s = sessionFactory.openSession();
        try {
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
            passwordAuth.setType("password");
            passwordAuth.setClient(servletClient);

            // Create the scopes
            List<ApplicationScope> scopes = new ArrayList<>();
            for (String scope : Scope.allScopes()) {
                ApplicationScope newScope = new ApplicationScope();
                newScope.setApplication(servletApp);
                newScope.setName(scope);
                scopes.add(newScope);
            }

            // Create the roles.
            Role adminRole = new Role();
            adminRole.setName("admin");
            adminRole.setApplication(servletApp);
            adminRole.setScopes(scopes);

            Role memberRole = new Role();
            memberRole.setName("member");
            memberRole.setApplication(servletApp);
            memberRole.setScopes(scopes);

            // Create the first admin
            User adminUser = new User();
            adminUser.setApplication(servletApp);
            adminUser.setRole(adminRole);

            // Create the admin's login identity.
            UserIdentity adminIdentity = new UserIdentity();
            adminIdentity.setAuthenticator(passwordAuth);
            adminIdentity.setRemoteId("admin");
            adminIdentity.setUser(adminUser);
            adminIdentity.setSalt(PasswordUtil.createSalt());
            adminIdentity.setPassword(
                    PasswordUtil.hash("admin", adminIdentity.getSalt()));

            Transaction t = s.beginTransaction();
            s.save(servletApp);
            s.save(servletClient);
            s.save(passwordAuth);
            scopes.forEach(s::save);
            s.save(adminRole);
            s.save(memberRole);
            s.save(adminUser);
            s.save(adminIdentity);
            t.commit();

            logger.info(String.format("Application ID: %s",
                    servletApp.getId()));
            logger.info(String.format("Admin User ID: %s",
                    adminUser.getId()));
            logger.info("Application created. Let's rock!");

            // Refresh the servlet app, populating all persisted references.
            s.refresh(servletApp);

            return servletApp;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Unable to persist kangaroo admin "
                    + "application.", e);
        } finally {
            s.close();
        }
    }

    /**
     * Invoked at the {@link Container container} start-up. This method is
     * invoked even
     * when application is reloaded and new instance of application has
     * started.
     *
     * @param container container that has been started.
     */
    @Override
    public void onStartup(final Container container) {
        Boolean firstRun = servletConfig.getBoolean(Config.FIRST_RUN, false);
        if (!firstRun) {
            Application app = bootstrapApplication();

            servletConfig.addProperty(Config.FIRST_RUN, true);
            servletConfig.addProperty(Config.APPLICATION_ID, app.getId());
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
