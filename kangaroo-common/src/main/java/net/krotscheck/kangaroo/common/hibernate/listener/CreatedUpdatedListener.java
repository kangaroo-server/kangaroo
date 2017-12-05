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

import net.krotscheck.kangaroo.common.hibernate.entity.ICreatedDateEntity;
import net.krotscheck.kangaroo.common.hibernate.entity.IModifiedDateEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

import javax.inject.Singleton;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Hibernate persistance listener that annotates createdDate and updatedDate,
 * ensure that they're always properly maintained.
 *
 * @author Michael Krotscheck
 */
public final class CreatedUpdatedListener
        implements PreInsertEventListener, PreUpdateEventListener {

    /**
     * The timezone. Everything we're doing is UTC.
     */
    private final TimeZone timeZone = TimeZone.getTimeZone("UTC");

    /**
     * Before insert, update the createdDate and modifiedDate.
     *
     * @param event The event.
     * @return false - continue persisting.
     */
    @Override
    public boolean onPreInsert(final PreInsertEvent event) {
        Object entity = event.getEntity();
        Object[] state = event.getState();
        Calendar now = Calendar.getInstance(timeZone);
        // Some databases don't support milliseconds.
        now.set(Calendar.MILLISECOND, 0);

        if (entity instanceof ICreatedDateEntity) {
            String[] propertyNames = event.getPersister().getEntityMetamodel()
                    .getPropertyNames();
            ICreatedDateEntity persistingEntity = (ICreatedDateEntity) entity;
            persistingEntity.setCreatedDate(now);
            setValue(state, propertyNames, "createdDate",
                    persistingEntity.getCreatedDate());
        }

        if (entity instanceof IModifiedDateEntity) {
            String[] propertyNames = event.getPersister().getEntityMetamodel()
                    .getPropertyNames();
            IModifiedDateEntity modifiedEntity = (IModifiedDateEntity) entity;
            modifiedEntity.setModifiedDate(now);

            setValue(state, propertyNames, "modifiedDate",
                    modifiedEntity.getModifiedDate());
        }

        return false;
    }

    /**
     * Before update, update the modifiedDate.
     *
     * @param event The event.
     * @return false - continue persisting.
     */
    @Override
    public boolean onPreUpdate(final PreUpdateEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof IModifiedDateEntity) {
            String[] propertyNames = event.getPersister().getEntityMetamodel()
                    .getPropertyNames();
            Object[] state = event.getState();

            Calendar now = Calendar.getInstance(timeZone);
            IModifiedDateEntity persistingEntity = (IModifiedDateEntity) entity;
            persistingEntity.setModifiedDate(now);

            setValue(state, propertyNames, "modifiedDate",
                    persistingEntity.getModifiedDate());
        }

        return false;
    }

    /**
     * Set a specific value in the hibernate persistence array.
     *
     * @param currentState  The current persistence state.
     * @param propertyNames The list of property names.
     * @param propertyToSet The name of the property to set.
     * @param value         The value to set.
     */
    private void setValue(final Object[] currentState,
                          final String[] propertyNames,
                          final String propertyToSet,
                          final Object value) {
        int index = ArrayUtils.indexOf(propertyNames, propertyToSet);
        currentState[index] = value;
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(CreatedUpdatedListener.class)
                    .to(PreInsertEventListener.class)
                    .to(PreUpdateEventListener.class)
                    .in(Singleton.class);
        }
    }
}
