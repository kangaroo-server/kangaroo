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

package net.krotscheck.kangaroo.common.hibernate.factory;

import net.krotscheck.kangaroo.server.Config;
import org.apache.commons.configuration.Configuration;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * This factory creates a singleton hibernate service registry object, using
 * hibernate's default configuration mechanism (hibernate.properties and
 * hibernate.cfg.xml). To ensure  all of your entities are properly registered,
 * add both of those files to your resources directory and ensure they reflect
 * the correct settings for your application.
 *
 * @author Michael Krotscheck
 */
public final class HibernateServiceRegistryFactory
        implements DisposableSupplier<ServiceRegistry> {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(HibernateServiceRegistryFactory.class);

    /**
     * The default configuration values.
     */
    private final Map<String, String> defaultSettings;

    /**
     * Constructor. Initializes the configuration.
     *
     * @param config Injected system configuration.
     * @throws IOException Thrown if the index directory cannot
     *                     be created.
     */
    @Inject
    public HibernateServiceRegistryFactory(@Named("system")
                                               final Configuration config)
            throws IOException {

        // Get the working directory.
        String workingDir = config.getString(Config.WORKING_DIR.getKey(),
                Config.WORKING_DIR.getValue());

        defaultSettings = new HashMap<>();
        defaultSettings.put("hibernate.connection.url",
                String.format("jdbc:h2:file:%s/h2.db", workingDir));
        defaultSettings.put("hibernate.connection.username", "oid");
        defaultSettings.put("hibernate.connection.password", "oid");
        defaultSettings.put("hibernate.connection.driver_class",
                "org.h2.Driver");
        defaultSettings.put("hibernate.dialect",
                "org.hibernate.dialect.H2Dialect");


        // Configure default values for the search index.
        File indexDir = new File(workingDir, "lucene_indexes");
        if (!Files.exists(indexDir.toPath())) {
            Files.createDirectories(indexDir.toPath());
        }

        defaultSettings.put("hibernate.search.default.directory_provider",
                "filesystem");
        defaultSettings.put("hibernate.search.default.indexBase",
                indexDir.getAbsolutePath());
    }

    /**
     * Provide a Hibernate Service Registry object.
     *
     * @return The hibernate serfice registry.
     */
    @Override
    public ServiceRegistry get() {
        logger.trace("Service Registry provide");

        return new StandardServiceRegistryBuilder()
                .configure() // configures settings from hibernate.cfg.xml
                .applySettings(defaultSettings) // Apply defaults
                .applySettings(System.getenv()) // Override from the env.
                .applySettings(System.getProperties()) // Override from JVM.
                .build();
    }

    /**
     * Dispose of the hibernate configuration.
     *
     * @param serviceRegistry The service registry to dispose of.
     */
    @Override
    public void dispose(final ServiceRegistry serviceRegistry) {
        StandardServiceRegistryBuilder.destroy(serviceRegistry);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(HibernateServiceRegistryFactory.class)
                    .to(ServiceRegistry.class)
                    .in(Singleton.class);
        }
    }
}
