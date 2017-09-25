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

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * This factory provides a singleton Hibernate SessionFactory for your
 * application context. You may use this to generate your own sessions if you
 * are not in an explicit request context, however if you do so please make sure
 * to clean up after yourself and call session.close().
 *
 * Furthermore, this injector will search through the ServiceLocator context to
 * determine if there are any Hibernate Event Listeners injected from other
 * features. It will gather those and automatically register them against the
 * hibernate factory.
 *
 * @author Michael Krotscheck
 */
public final class HibernateSessionFactoryFactory
        implements DisposableSupplier<SessionFactory> {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(HibernateSessionFactoryFactory.class);
    /**
     * Injected hibernate configuration.
     */
    private ServiceRegistry serviceRegistry;
    /**
     * The injector.
     */
    private InjectionManager injectionManager;

    /**
     * Create a new factory factory.
     *
     * @param registry The Hibernate Service Registry
     * @param iManager The injection manager.
     */
    @Inject
    public HibernateSessionFactoryFactory(final ServiceRegistry registry,
                                          final InjectionManager iManager) {
        this.serviceRegistry = registry;
        this.injectionManager = iManager;
    }

    /**
     * Provide a singleton instance of the hibernate session factory.
     *
     * @return A session factory.
     */
    @Override
    public SessionFactory get() {
        logger.trace("Creating hibernate session factory.");

        // Build the service registry.
        SessionFactory factory = new MetadataSources(serviceRegistry)
                .buildMetadata()
                .buildSessionFactory();

        // Register our event listeners.
        injectEventListeners(((SessionFactoryImpl) factory)
                .getServiceRegistry());

        return factory;
    }

    /**
     * Dispose of the hibernate session.
     *
     * @param sessionFactory The session to dispose.
     */
    @Override
    public void dispose(final SessionFactory sessionFactory) {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            logger.debug("Disposing of hibernate session factory.");
            sessionFactory.close();
        }
    }

    /**
     * This method automatically adds discovered hibernate event listeners into
     * the hibernate service registry.
     *
     * @param registry The service registry.
     */
    private void injectEventListeners(final ServiceRegistry registry) {

        EventListenerRegistry eventRegistry = registry
                .getService(EventListenerRegistry.class);

        List<PostInsertEventListener> postInsertEvents = injectionManager
                .getAllInstances(PostInsertEventListener.class);
        for (PostInsertEventListener piEventListener : postInsertEvents) {
            logger.trace("Registering PostInsert: " + piEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.POST_INSERT,
                    piEventListener);
        }

        List<PostUpdateEventListener> postUpdateEvents = injectionManager
                .getAllInstances(PostUpdateEventListener.class);
        for (PostUpdateEventListener puEventListener : postUpdateEvents) {
            logger.trace("Registering PostUpdate: " + puEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.POST_UPDATE,
                    puEventListener);
        }

        List<PostDeleteEventListener> postDeleteEvents = injectionManager
                .getAllInstances(PostDeleteEventListener.class);
        for (PostDeleteEventListener pdEventListener : postDeleteEvents) {
            logger.trace("Registering PostDelete: " + pdEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.POST_DELETE,
                    pdEventListener);
        }

        List<PreInsertEventListener> preInsertEvents = injectionManager
                .getAllInstances(PreInsertEventListener.class);
        for (PreInsertEventListener piEventListener : preInsertEvents) {
            logger.trace("Registering PreInsert: " + piEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.PRE_INSERT,
                    piEventListener);
        }

        List<PreUpdateEventListener> preUpdateEvents = injectionManager
                .getAllInstances(PreUpdateEventListener.class);
        for (PreUpdateEventListener puEventListener : preUpdateEvents) {
            logger.trace("Registering PreUpdate: " + puEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.PRE_UPDATE,
                    puEventListener);
        }

        List<PreDeleteEventListener> preDeleteEvents = injectionManager
                .getAllInstances(PreDeleteEventListener.class);
        for (PreDeleteEventListener pdEventListener : preDeleteEvents) {
            logger.trace("Registering PreDelete: " + pdEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.PRE_DELETE,
                    pdEventListener);
        }

        List<PostCommitInsertEventListener> pciEvents = injectionManager
                .getAllInstances(PostCommitInsertEventListener.class);
        for (PostCommitInsertEventListener cpiEventListener : pciEvents) {
            logger.trace("Registering PostCommitInsert: " + cpiEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.POST_COMMIT_INSERT,
                    cpiEventListener);
        }

        List<PostCommitUpdateEventListener> pcuEvents = injectionManager
                .getAllInstances(PostCommitUpdateEventListener.class);
        for (PostCommitUpdateEventListener cpuEventListener : pcuEvents) {
            logger.trace("Registering PostCommitUpdate: " + cpuEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.POST_COMMIT_UPDATE,
                    cpuEventListener);
        }

        List<PostCommitDeleteEventListener> pcdEvents = injectionManager
                .getAllInstances(PostCommitDeleteEventListener.class);
        for (PostCommitDeleteEventListener cpdEventListener : pcdEvents) {
            logger.trace("Registering PostCommitDelete: " + cpdEventListener
                    .getClass().getCanonicalName());
            eventRegistry.appendListeners(EventType.POST_COMMIT_DELETE,
                    cpdEventListener);
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(HibernateSessionFactoryFactory.class)
                    .to(SessionFactory.class)
                    .in(Singleton.class);
        }
    }
}
