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

package net.krotscheck.kangaroo.common.hibernate.entity;


import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.Size;

/**
 * A test hibernate persistence entity.
 *
 * @author Michael Krotscheck
 */
@Entity
@Indexed
@Table(name = "test")
public final class TestEntity extends AbstractEntity {

    /**
     * The name of the entity.
     */
    @Basic(optional = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @Column(name = "name", nullable = false)
    @Size(min = 3, max = 255, message = "Test entity name has constraints.")
    private String name;

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
