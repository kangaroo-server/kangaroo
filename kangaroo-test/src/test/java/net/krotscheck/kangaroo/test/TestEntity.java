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

package net.krotscheck.kangaroo.test;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;

import java.util.Calendar;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A test entity, copied verbatim from AbstractEntity.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "test")
@Indexed(index = "test")
public class TestEntity {

    /**
     * The DB ID.
     */
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Type(type = "uuid-binary")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @DocumentId
    private UUID id;

    /**
     * The date this record was created.
     */
    @Column(name = "createdDate")
    private Calendar createdDate;

    /**
     * The date this record was last modified.
     */
    @Column(name = "modifiedDate")
    private Calendar modifiedDate;

    /**
     * Return the DB record's ID.
     *
     * @return The id for this entity.
     */
    public final UUID getId() {
        return id;
    }

    /**
     * Set the ID.
     *
     * @param id The unique ID for this entity.
     */
    public final void setId(final UUID id) {
        this.id = id;
    }

    /**
     * Get the date on which this record was created.
     *
     * @return The created date.
     */
    public final Calendar getCreatedDate() {
        if (createdDate == null) {
            return null;
        } else {
            return (Calendar) createdDate.clone();
        }
    }

    /**
     * Set the date on which this record was created.
     *
     * @param date The creation date for this entity.
     */
    public final void setCreatedDate(final Calendar date) {
        this.createdDate = (Calendar) date.clone();
    }

    /**
     * Get the last modified date.
     *
     * @return The last time this record was modified, or null.
     */
    public final Calendar getModifiedDate() {
        if (modifiedDate == null) {
            return null;
        } else {
            return (Calendar) modifiedDate.clone();
        }
    }

    /**
     * Set the last modified date.
     *
     * @param date The modified date for this entity.
     */
    public final void setModifiedDate(final Calendar date) {
        this.modifiedDate = (Calendar) date.clone();
    }

    /**
     * Simplified Stringification.
     *
     * @return A string representation of the instance.
     */
    public final String toString() {
        return String.format("%s [id=%s]", this.getClass().getCanonicalName(),
                getId());
    }
}
