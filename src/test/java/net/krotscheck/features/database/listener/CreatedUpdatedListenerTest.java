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

package net.krotscheck.features.database.listener;

import net.krotscheck.features.database.entity.AbstractEntity;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.junit.Assert;
import org.junit.Test;

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
public final class CreatedUpdatedListenerTest {

    /**
     * Assert that a record has its created and modified date updated during an
     * insert.
     */
    @Test
    public void testOnPreInsert() {
        PreInsertEventListener listener = new CreatedUpdatedListener();
        PreInsertEvent event = mock(PreInsertEvent.class);
        AbstractEntity entity = new TestEntity();

        when(event.getEntity()).thenReturn(entity);

        Assert.assertNull(entity.getCreatedDate());
        Assert.assertNull(entity.getModifiedDate());
        Assert.assertFalse(listener.onPreInsert(event));
        Assert.assertNotNull(entity.getCreatedDate());
        Assert.assertNotNull(entity.getModifiedDate());
    }

    /**
     * Assert that a record has its modified date updated during an update.
     */
    @Test
    public void testOnPreUpdate() {
        PreUpdateEventListener listener = new CreatedUpdatedListener();
        PreUpdateEvent event = mock(PreUpdateEvent.class);
        AbstractEntity entity = new TestEntity();

        when(event.getEntity()).thenReturn(entity);

        Assert.assertNull(entity.getCreatedDate());
        Assert.assertNull(entity.getModifiedDate());
        Assert.assertFalse(listener.onPreUpdate(event));
        Assert.assertNull(entity.getCreatedDate());
        Assert.assertNotNull(entity.getModifiedDate());
    }

    /**
     * Assert that a record has its modified date updated during an update.
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

    /**
     * Test entity, used for testing!
     */
    private static class TestEntity extends AbstractEntity {

    }
}
