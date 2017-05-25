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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A configuration entity, containing key/value pairs sorted by group.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "configuration")
public final class ConfigurationEntry extends AbstractEntity {

    /**
     * The section for this entry.
     */
    @Basic(optional = false)
    @Column(name = "section", nullable = false)
    private String section;

    /**
     * The key name.
     */
    @Basic(optional = false)
    @Column(name = "configKey", nullable = false)
    private String key;

    /**
     * The stored value.
     */
    @Basic(optional = false)
    @Column(name = "configValue", nullable = false)
    private String value;

    /**
     * Get the configuration section.
     *
     * @return The configuration section.
     */
    public String getSection() {
        return section;
    }

    /**
     * Set the configuration section.
     *
     * @param section The configuration section.
     */
    public void setSection(final String section) {
        this.section = section;
    }

    /**
     * The key for this configuration entry.
     *
     * @return The entry key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the key for this configuration entry.
     *
     * @param key The key.
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * Get the value for this configuration entry.
     *
     * @return The value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value for this configuration entry.
     *
     * @param value The new value.
     */
    public void setValue(final String value) {
        this.value = value;
    }
}
