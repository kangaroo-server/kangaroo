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

package net.krotscheck.kangaroo.database.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.charfilter.MappingCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilterFactory;
import org.apache.lucene.analysis.miscellaneous.TrimFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Calendar;
import java.util.UUID;

/**
 * Generic entity, from which all other entities are born.
 *
 * @author Michael Krotscheck
 */
@MappedSuperclass
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
@AnalyzerDef(name = "entity_analyzer",
        charFilters = {
                @CharFilterDef(factory = HTMLStripCharFilterFactory.class),
                @CharFilterDef(factory = MappingCharFilterFactory.class,
                        params = {@Parameter(
                                name = "mapping",
                                value = "kangaroo-char-mapping.properties")}
                )
        },
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = TrimFilterFactory.class),
                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
                @TokenFilterDef(factory = StopFilterFactory.class),
                @TokenFilterDef(factory = SnowballPorterFilterFactory.class,
                        params = {
                                @Parameter(name = "language", value = "English")
                        }),
                @TokenFilterDef(
                        factory = RemoveDuplicatesTokenFilterFactory.class)
        })
public abstract class AbstractEntity implements Cloneable {

    /**
     * The DB ID.
     */
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Type(type = "uuid-binary")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
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
     * Equality implementation, global.
     *
     * @param o The object to test.
     * @return True if the ID's are equal, otherwise false.
     */
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !this.getClass().isInstance(o)) {
            return false;
        }

        // Cast
        AbstractEntity other = (AbstractEntity) o;

        // if the id is missing, return false
        if (id == null) {
            return false;
        }

        // equivalence by id
        return id.equals(other.getId());
    }

    /**
     * Public Hashcode generation.
     *
     * @return A hashcode for this entity.
     */
    public final int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getId())
                .append(this.getClass().getName())
                .toHashCode();
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

    /**
     * Clone this instance.
     *
     * @return A clone of this entity.
     */
    @Override
    public final Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
