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

package net.krotscheck.kangaroo.database.listener;

import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.test.DatabaseTest;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Assert that any persiste records have their respective dates kept
 * up-to-date.
 *
 * @author Michael Krotscheck
 */
public final class CreatedUpdatedListenerTest extends DatabaseTest {

    /**
     * Attach the event listener to our session factory.
     */
    @Before
    public void setup() {
        SessionFactory factory = getSessionFactory();

        // Register our event listeners.
        ServiceRegistry registry =
                ((SessionFactoryImpl) factory).getServiceRegistry();

        EventListenerRegistry eventRegistry = registry
                .getService(EventListenerRegistry.class);

        eventRegistry.appendListeners(EventType.PRE_INSERT,
                new CreatedUpdatedListener());
        eventRegistry.appendListeners(EventType.PRE_UPDATE,
                new CreatedUpdatedListener());
    }

    /**
     * Load data fixtures for each test.
     *
     * @param session The session to use to build the environment.
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<EnvironmentBuilder> fixtures(final Session session) {
        return null;
    }

    /**
     * Assert that a record has its created and modified date updated during an
     * insert.
     */
    @Test
    public void testOnPreInsert() {
        Application a = new Application();
        a.setName("foo");
        Assert.assertNull(a.getCreatedDate());
        Assert.assertNull(a.getModifiedDate());

        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.saveOrUpdate(a);
        t.commit();

        Assert.assertNotNull(a.getCreatedDate());
        Assert.assertNotNull(a.getModifiedDate());

        s.evict(a);

        Application readApplication =
                s.get(Application.class, a.getId());

        Assert.assertNotNull(readApplication.getCreatedDate());
        Assert.assertNotNull(readApplication.getModifiedDate());
    }

    /**
     * Assert that a record has its modified date updated during an update.
     *
     * @throws Exception Thrown when interrupted.
     */
    @Test
    public void testOnPreUpdate() throws Exception {
        Application a = new Application();
        a.setName("foo");

        Assert.assertNull(a.getCreatedDate());
        Assert.assertNull(a.getModifiedDate());

        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.saveOrUpdate(a);
        t.commit();

        // Evict and reload. This is to ensure that the entity's dates
        // matches what the underlying database supports. Mysql, for
        // instance, doesn't store milliseconds.
        s.refresh(a);

        Calendar created = (Calendar) a.getCreatedDate().clone();
        Calendar modified = (Calendar) a.getModifiedDate().clone();

        Assert.assertNotNull(created);
        Assert.assertNotNull(modified);

        TimeUnit.SECONDS.sleep(1);

        a.setName("bar");
        Transaction t2 = s.beginTransaction();
        s.saveOrUpdate(a);
        t2.commit();

        s.refresh(a);

        Assert.assertEquals(created, a.getCreatedDate());
        Assert.assertNotEquals(modified, a.getModifiedDate());

        s.evict(a);

        Application readApplication = s.get(Application.class, a.getId());

        Assert.assertEquals(created, readApplication.getCreatedDate());
        Assert.assertNotEquals(modified, readApplication.getModifiedDate());

        Assert.assertEquals(a.getCreatedDate(),
                readApplication.getCreatedDate());
        Assert.assertEquals(a.getModifiedDate(),
                readApplication.getModifiedDate());
    }

    /**
     * Assert that non-AbstractEntities aren't touched.
     */
    @Test
    public void testNotAbstractEntity() {
        CreatedUpdatedListener listener = new CreatedUpdatedListener();
        PreUpdateEvent updateEvent = mock(PreUpdateEvent.class);
        PreInsertEvent insertEvent = mock(PreInsertEvent.class);
        Object entity = spy(Object.class);

        when(updateEvent.getEntity()).thenReturn(entity);
        when(insertEvent.getEntity()).thenReturn(entity);

        listener.onPreInsert(insertEvent);
        verifyZeroInteractions(entity);

        listener.onPreUpdate(updateEvent);
        verifyZeroInteractions(entity);
    }
}
