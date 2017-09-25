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

package net.krotscheck.kangaroo.authz.admin.v1.test.rule;

import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.migration.DatabaseMigrationState;
import net.krotscheck.kangaroo.test.rule.HibernateResource;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * This JUnit rule bootstraps a common set of data against which we can run
 * our tests.
 *
 * @author Michael Krotscheck
 */
public final class TestDataResource
        extends net.krotscheck.kangaroo.test.rule.TestDataResource {

    /**
     * The admin application context.
     */
    private ApplicationContext adminContext;

    /**
     * The second application context id.
     */
    private ApplicationContext secondContext;

    /**
     * The system configuration.
     */
    private Configuration systemConfig;

    /**
     * Create a new instance of the test data resource.
     *
     * @param factoryProvider The session factory provider.
     */
    public TestDataResource(final HibernateResource factoryProvider) {
        super(factoryProvider);
    }

    /**
     * Return the context of the current admin application.
     *
     * @return The admin application context.
     */
    public ApplicationContext getAdminApplication() {
        return adminContext;
    }

    /**
     * Return the context of the secondary application.
     *
     * @return The other application context.
     */
    public ApplicationContext getSecondaryApplication() {
        return secondContext;
    }

    /**
     * Get the system configuration.
     *
     * @return The system config for this test suite.
     */
    public Configuration getSystemConfiguration() {
        return systemConfig;
    }

    /**
     * Create the test application using the FirstRun context listener. This
     * is to prevent it running when the application starts up.
     *
     * @return The UUID of the admin application that was created.
     */
    private UUID createAdminApplication() {
        SessionFactory factory = getSessionFactory();

        // Initialize the servlet configuration.
        ServletConfigFactory configFactory = new ServletConfigFactory(factory);
        systemConfig = configFactory.get();

        // Initialize the application.
        FirstRunContainerLifecycleListener listener =
                new FirstRunContainerLifecycleListener(factory, systemConfig,
                        new DatabaseMigrationState());
        listener.onStartup(null);
        listener.onShutdown(null);

        // Store the application id.
        return UUID.fromString(systemConfig.getString(Config.APPLICATION_ID));
    }

    /**
     * Load data into the database.
     */
    @Override
    protected void loadTestData(final Session session) {
        UUID adminAppId = createAdminApplication();

        ApplicationBuilder adminBuilder = ApplicationBuilder
                .fromApplication(session, adminAppId)
                .user()
                .identity();
        ApplicationContext initialContext = adminBuilder.build();
        applyUsersToClient(adminBuilder); // Add more users to the admin app.
        buildApplicationData(adminBuilder);
        adminContext = adminBuilder.build();

        // Create a second app, owned by another user.
        ApplicationBuilder secondBuilder = ApplicationBuilder
                .newApplication(session)
                .owner(initialContext.getUser())
                .scopes(Scope.allScopes());
        buildApplicationData(secondBuilder);
        secondContext = secondBuilder.build();

        // Create a whole lot of applications to run some tests against.
        List<Application> applications = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String appName = String.format("Application %s- %s", i, i % 2 == 0
                    ? "many" : "frown");
            Application one = new Application();
            one.setName(appName);
            one.setOwner(adminContext.getOwner());
            applications.add(one);

            Application two = new Application();
            two.setName(appName);
            two.setOwner(secondContext.getOwner());
            applications.add(two);
        }
        Application singleOne = new Application();
        singleOne.setName("Single");
        singleOne.setOwner(adminContext.getOwner());
        applications.add(singleOne);

        Application singleTwo = new Application();
        singleTwo.setName("Single");
        singleTwo.setOwner(secondContext.getOwner());
        applications.add(singleTwo);

        session.getTransaction().begin();
        applications.forEach(session::save);
        session.getTransaction().commit();
    }

    /**
     * Build test data for this application.
     *
     * @param builder The builder to modify.
     */
    private void buildApplicationData(final ApplicationBuilder builder) {
        // Add some data for scopes
        builder.scope("Single Scope")
                .scope("Second Scope - many")
                .scope("Third Scope - many")
                .scope("Fourth Scope - many");
        builder.role("Single Role")
                .role("Second Role - many")
                .role("Third Role - many")
                .role("Fourth Role - many");

        builder.client(ClientType.ClientCredentials,
                "Single client")
                .authenticator(AuthenticatorType.Test);

        builder.client(ClientType.OwnerCredentials,
                "Second client - many")
                .authenticator(AuthenticatorType.Password);
        applyUsersToClient(builder);

        builder.client(ClientType.Implicit,
                "Third client - many")
                .authenticator(AuthenticatorType.Password);
        applyUsersToClient(builder);

        builder.client(ClientType.AuthorizationGrant,
                "Fourth client - many")
                .authenticator(AuthenticatorType.Password);
        applyUsersToClient(builder);
    }


    /**
     * Helper method: Apply some users and identities to the client in a
     * provided context.
     *
     * @param builder The builder to add these entities to.
     */
    private void applyUsersToClient(final ApplicationBuilder builder) {
        // Create some users
        builder.user()
                .identity()
                .claim("name", "Single User")
                .redirect("http://single.token.example.com/")
                .referrer("http://single.token.example.com/")
                .authToken()
                .bearerToken();
        builder.user()
                .identity()
                .claim("name", "Second User - many")
                .redirect("http://second.token.example.com/many")
                .referrer("http://second.token.example.com/many")
                .authToken()
                .bearerToken();
        builder.user()
                .identity()
                .claim("name", "Third User - many")
                .redirect("http://third.token.example.com/many")
                .referrer("http://third.token.example.com/many")
                .authToken()
                .bearerToken();
        builder.user()
                .identity()
                .claim("name", "Fourth User - many")
                .redirect("http://fourth.token.example.com/many")
                .referrer("http://fourth.token.example.com/many")
                .authToken()
                .bearerToken();
    }
}
