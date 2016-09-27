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

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
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
        if (entity instanceof AbstractEntity) {
            String[] propertyNames = event.getPersister().getEntityMetamodel()
                    .getPropertyNames();
            Object[] state = event.getState();

            AbstractEntity persistingEntity = (AbstractEntity) entity;
            persistingEntity.setCreatedDate(Calendar.getInstance(timeZone));
            persistingEntity.setModifiedDate(Calendar.getInstance(timeZone));

            setValue(state, propertyNames, "createdDate",
                    persistingEntity.getCreatedDate());
            setValue(state, propertyNames, "modifiedDate",
                    persistingEntity.getModifiedDate());
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
        if (entity instanceof AbstractEntity) {
            String[] propertyNames = event.getPersister().getEntityMetamodel()
                    .getPropertyNames();
            Object[] state = event.getState();

            AbstractEntity persistingEntity = (AbstractEntity) entity;
            persistingEntity.setModifiedDate(Calendar.getInstance(timeZone));

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
