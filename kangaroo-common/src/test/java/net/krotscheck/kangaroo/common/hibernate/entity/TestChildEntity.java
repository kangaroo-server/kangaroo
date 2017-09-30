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

import net.krotscheck.kangaroo.common.hibernate.id.AbstractEntityReferenceDeserializer;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A generic child entity.
 *
 * @author Michael Krotscheck
 */
@Entity
@Indexed
@Table(name = "child")
public final class TestChildEntity extends AbstractEntity {

    /**
     * Deserialize a reference to an User.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<TestChildEntity> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(TestChildEntity.class);
        }
    }
}
