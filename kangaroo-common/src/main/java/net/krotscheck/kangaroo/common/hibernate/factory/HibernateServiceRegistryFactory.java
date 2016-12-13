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

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

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
        implements Factory<ServiceRegistry> {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(HibernateServiceRegistryFactory.class);

    /**
     * Provide a Hibernate Service Registry object.
     *
     * @return The hibernate serfice registry.
     */
    @Override
    public ServiceRegistry provide() {
        logger.trace("Service Registry provide");

        return new StandardServiceRegistryBuilder()
                .configure() // configures settings from hibernate.cfg.xml
                .applySettings(System.getProperties())
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
