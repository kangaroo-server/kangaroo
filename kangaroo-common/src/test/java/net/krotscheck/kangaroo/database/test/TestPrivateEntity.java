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

package net.krotscheck.kangaroo.database.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.User;

import javax.persistence.Transient;

/**
 * A child entity with a private constructor.
 *
 * @author Michael Krotscheck
 */
public final class TestPrivateEntity extends AbstractEntity {

    /**
     * Private constructor.
     */
    private TestPrivateEntity() {

    }

    /**
     * The owner of this entity.
     *
     * @return This entity's owner, if it exists.
     */
    @Override
    @Transient
    @JsonIgnore
    public User getOwner() {
        return null;
    }
}
