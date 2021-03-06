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

import net.krotscheck.kangaroo.common.hibernate.config.HibernateConfiguration;
import org.apache.commons.configuration.Configuration;


import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Supplier;

/**
 * This factory instantiates a configuration element which contains all
 * system-level configuration elements, such as root URI path, Admin API ID,
 * and others. It should only ever be used for configuration required by this
 * servlet.
 *
 * @author Michael Krotscheck
 */
public final class ServletConfigFactory
        implements Supplier<Configuration> {

    /**
     * The name of the configuration group.
     */
    public static final String GROUP_NAME = "kangaroo-servlet-admin";

    /**
     * The Session factory that will be provided to the hibernate
     * configuration.
     */
    private final SessionFactory factory;

    /**
     * Create a new factory instance.
     *
     * @param factory The hibernate session factory.
     */
    @Inject
    public ServletConfigFactory(final SessionFactory factory) {
        this.factory = factory;
    }

    /**
     * This method will create instances of the type of this factory.  The
     * provide method must be annotated with the desired scope and qualifiers.
     *
     * @return The produces object
     */
    @Override
    public Configuration get() {
        return new HibernateConfiguration(factory, GROUP_NAME);
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(ServletConfigFactory.class)
                    .to(Configuration.class)
                    .named(GROUP_NAME)
                    .in(Singleton.class);
        }
    }
}
