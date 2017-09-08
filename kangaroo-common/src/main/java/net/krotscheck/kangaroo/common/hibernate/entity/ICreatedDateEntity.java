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

import java.util.Calendar;

/**
 * This interface describes a database entity that adheres to the
 * 'createdDate' contract.
 *
 * @author Michael Krotscheck
 */
public interface ICreatedDateEntity {

    /**
     * Get the date on which this record was created.
     *
     * @return The created date.
     */
    Calendar getCreatedDate();

    /**
     * Set the date on which this record was created.
     *
     * @param date The creation date for this entity.
     */
    void setCreatedDate(Calendar date);
}
