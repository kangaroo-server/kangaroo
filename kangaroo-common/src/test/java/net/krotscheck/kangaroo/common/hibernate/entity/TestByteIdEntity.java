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

package net.krotscheck.kangaroo.common.hibernate.entity;


import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.util.Calendar;

/**
 * A test hibernate persistence entity.
 *
 * @author Michael Krotscheck
 */
@Entity
@Indexed
@Table(name = "test")
public final class TestByteIdEntity {
    /**
     * A 64-byte id that identifies this particular session.
     */
    @Id
    @GenericGenerator(name = "secure_random_bytes",
            strategy = "net.krotscheck.kangaroo.common.hibernate.id"
                    + ".SecureRandomIdGenerator")
    @GeneratedValue(generator = "secure_random_bytes")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private byte[] id = null;

    /**
     * The date this record was created.
     */
    @Column(name = "createdDate")
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".CalendarTimestampType")
    private Calendar createdDate;

    /**
     * The date this record was last modified.
     */
    @Column(name = "modifiedDate")
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".CalendarTimestampType")
    private Calendar modifiedDate;

    /**
     * The name of the entity.
     */
    @Basic(optional = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @Column(name = "name", nullable = false)
    @Size(min = 3, max = 255, message = "Test entity name has constraints.")
    private String name;

    /**
     * Return the DB record's ID.
     *
     * @return The id for this entity.
     */
    public byte[] getId() {
        return id;
    }

    /**
     * Set the ID.
     *
     * @param id The unique ID for this entity.
     */
    public void setId(final byte[] id) {
        this.id = id;
    }

    /**
     * Get the date on which this record was created.
     *
     * @return The created date.
     */
    public Calendar getCreatedDate() {
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
    public void setCreatedDate(final Calendar date) {
        if (date == null) {
            this.createdDate = null;
        } else {
            this.createdDate = (Calendar) date.clone();
        }
    }

    /**
     * Get the last modified date.
     *
     * @return The last time this record was modified, or null.
     */
    public Calendar getModifiedDate() {
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
    public void setModifiedDate(final Calendar date) {
        if (date == null) {
            this.modifiedDate = null;
        } else {
            this.modifiedDate = (Calendar) date.clone();
        }
    }

    /**
     * Get the name for this entity.
     *
     * @return The entity's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for this entity.
     *
     * @param name A new name.
     */
    public void setName(final String name) {
        this.name = name;
    }
}
