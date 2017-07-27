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

package net.krotscheck.kangaroo.common.hibernate.listener;

import net.krotscheck.kangaroo.common.hibernate.entity.TestEntity;
import net.krotscheck.kangaroo.test.jerseyTest.DatabaseTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerRegistrationException;
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

        try {
            eventRegistry.appendListeners(EventType.PRE_INSERT,
                    new CreatedUpdatedListener());
            eventRegistry.appendListeners(EventType.PRE_UPDATE,
                    new CreatedUpdatedListener());
        } catch (EventListenerRegistrationException e) {
            // Session Factories are class-static, so we may have already
            // registered these listeners. i.e. do nothing.
        }
    }

    /**
     * Assert that a record has its created and modified date updated during an
     * insert.
     */
    @Test
    public void testOnPreInsert() {
        TestEntity a = new TestEntity();
        a.setName("foo");
        Assert.assertNull(a.getCreatedDate());
        Assert.assertNull(a.getModifiedDate());

        Session s = getSession();
        s.getTransaction().begin();
        s.saveOrUpdate(a);
        s.getTransaction().commit();

        Assert.assertNotNull(a.getCreatedDate());
        Assert.assertNotNull(a.getModifiedDate());

        s.evict(a);

        TestEntity testEntity = s.get(TestEntity.class, a.getId());

        Assert.assertNotNull(testEntity.getCreatedDate());
        Assert.assertNotNull(testEntity.getModifiedDate());
    }

    /**
     * Assert that a record has its modified date updated during an update.
     *
     * @throws InterruptedException Thrown if the thread is interrupted (it
     *                              shouldn't be).
     */
    @Test
    public synchronized void testOnPreUpdate() throws InterruptedException {
        TestEntity a = new TestEntity();
        a.setName("foo");

        // Save, generate the dates.
        Session s = getSession();
        s.getTransaction().begin();
        s.saveOrUpdate(a);
        s.getTransaction().commit();

        // Load values back in from the database - some databases drop the
        // miliseconds, so we make sure we test with a clean, database-native
        // value.
        s.evict(a);

        s.getTransaction().begin();
        a = s.get(TestEntity.class, a.getId());
        s.getTransaction().commit();

        // Store our dates for later checking.
        Calendar created = a.getCreatedDate();
        Calendar modified = a.getModifiedDate();

        // Wait for 1 second, since time resolution is not precise in some
        // databases.
        this.wait(1000);

        // Run an update.
        a.setName("bar");
        s.getTransaction().begin();
        s.saveOrUpdate(a);
        s.getTransaction().commit();

        // Make sure that the createdDate has not changed, but the modified
        // date has.
        Assert.assertEquals(created, a.getCreatedDate());
        Assert.assertNotEquals(modified, a.getModifiedDate());
        Calendar newModifiedDate = a.getModifiedDate();

        // Evict and refresh, make sure that the above values have persisted.
        s.evict(a);
        TestEntity testEntity = s.get(TestEntity.class, a.getId());

        Assert.assertEquals(created, testEntity.getCreatedDate());
        Assert.assertEquals(newModifiedDate, testEntity.getModifiedDate());
        Assert.assertNotEquals(modified, testEntity.getModifiedDate());
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
