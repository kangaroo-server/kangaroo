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

package net.krotscheck.kangaroo.database.config;

import net.krotscheck.kangaroo.database.entity.ConfigurationEntry;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An apache-commons configuration implementation backed by hibernate entities.
 *
 * This implementation is not thread safe.
 *
 * @author Michael Krotscheck
 */
public final class HibernateConfiguration
        extends AbstractConfiguration
        implements Configuration {

    /**
     * The session factory.
     */
    private final SessionFactory factory;

    /**
     * The name of the configuration section.
     */
    private final String section;

    /**
     * Cache of last-loaded configuration values.
     */
    private SortedMap<String, Object> configurationCache;

    /**
     * Create a new instance of this configuration implementation, for a
     * specific group of configuration values.
     *
     * @param factory The session factory used to access sessions.
     * @param section The name of the section. Corresponds to the 'section'
     *                column in the configuration table.
     */
    public HibernateConfiguration(final SessionFactory factory,
                                  final String section) {
        this.factory = factory;
        this.section = section;
    }

    /**
     * Adds a key/value pair to the Configuration. Override this method to
     * provide write access to underlying Configuration store.
     *
     * @param key   key to use for mapping
     * @param value object to store
     */
    @Override
    protected void addPropertyDirect(final String key,
                                     final Object value) {
        ConfigurationEntry e = new ConfigurationEntry();
        e.setSection(section);
        e.setKey(key);
        e.setValue(String.valueOf(value));

        // Open a new session, save the data, and close the session.
        Session s = factory.openSession();
        Transaction t = s.beginTransaction();
        s.save(e);
        t.commit();
        s.close();

        // Clear the existing configuration
        configurationCache = null;
    }

    /**
     * Check if the configuration is empty.
     *
     * @return {@code true} if the configuration contains no property,
     * {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        return getConfiguration().isEmpty();
    }

    /**
     * Check if the configuration contains the specified key.
     *
     * @param key the key whose presence in this configuration is to be tested
     * @return {@code true} if the configuration contains a value for this
     * key, {@code false} otherwise
     */
    @Override
    public boolean containsKey(final String key) {
        return getConfiguration().containsKey(key);
    }

    /**
     * Gets a property from the configuration. This is the most basic get
     * method for retrieving values of properties. In a typical implementation
     * of the {@code Configuration} interface the other get methods (that
     * return specific data types) will internally make use of this method. On
     * this level variable substitution is not yet performed. The returned
     * object is an internal representation of the property value for the
     * passed
     * in key. It is owned by the {@code Configuration} object. So a caller
     * should not modify this object. It cannot be guaranteed that this object
     * will stay constant over time (i.e. further update operations on the
     * configuration may change its internal state).
     *
     * @param key property to retrieve
     * @return the value to which this configuration maps the specified key, or
     * null if the configuration contains no mapping for this key.
     */
    @Override
    public Object getProperty(final String key) {
        return getConfiguration().get(key);
    }

    /**
     * Removes the specified property from this configuration. This method is
     * called by {@code clearProperty()} after it has done some
     * preparations. It should be overridden in sub classes. This base
     * implementation is just left empty.
     *
     * @param key the key to be removed
     */
    @Override
    protected void clearPropertyDirect(final String key) {

        // Open a session.
        Session s = factory.openSession();

        // Search for all configuration entries.
        Criteria c = s.createCriteria(ConfigurationEntry.class);
        c.add(Restrictions.eq("section", section));
        c.add(Restrictions.eq("key", key));

        Transaction t = s.beginTransaction();
        for (ConfigurationEntry entry : (List<ConfigurationEntry>) c.list()) {
            s.delete(entry);
        }
        t.commit();

        // Close the session.
        s.close();

        // Clear the existing configuration
        configurationCache = null;
    }

    /**
     * Clear all configuration entries for this particular group.
     */
    @Override
    public void clear() {
        fireEvent(EVENT_CLEAR, null, null, true);

        // Open a session.
        Session s = factory.openSession();

        // Search for all configuration entries.
        Criteria c = s.createCriteria(ConfigurationEntry.class);
        c.add(Restrictions.eq("section", section));

        Transaction t = s.beginTransaction();
        for (ConfigurationEntry entry : (List<ConfigurationEntry>) c.list()) {
            s.delete(entry);
        }
        t.commit();

        // Close the session.
        s.close();

        // Clear the existing configuration
        configurationCache = null;

        // Fire the completion event.
        fireEvent(EVENT_CLEAR, null, null, false);
    }

    /**
     * Get the list of the keys contained in the configuration. The returned
     * iterator can be used to obtain all defined keys. Note that the exact
     * behavior of the iterator's {@code remove()} method is specific to
     * a concrete implementation. It <em>may</em> remove the corresponding
     * property from the configuration, but this is not guaranteed. In any case
     * it is no replacement for calling
     * {@link #clearProperty(String)} for this property. So it is
     * highly recommended to avoid using the iterator's {@code remove()}
     * method.
     *
     * @return An Iterator.
     */
    @Override
    public Iterator<String> getKeys() {
        return getConfiguration().keySet().iterator();
    }

    /**
     * Retrieve the most recent configuration elements for this group.
     *
     * @return The configuration, could be cached.
     */
    private SortedMap<String, Object> getConfiguration() {
        if (configurationCache == null) {
            // Open a session.
            Session s = factory.openSession();

            // Search for all configuration entries.
            Criteria c = s.createCriteria(ConfigurationEntry.class);
            c.add(Restrictions.eq("section", section));
            List<ConfigurationEntry> entries = c.list();

            /**
             * Map those entries into a tree.
             */
            TreeMap<String, Object> values = new TreeMap<>();
            for (ConfigurationEntry entry : entries) {
                values.put(entry.getKey(), entry.getValue());
            }

            // Close the session.
            s.close();

            // Store the cache as an unmodifiable map.
            configurationCache = Collections.unmodifiableSortedMap(values);
        }
        return configurationCache;
    }
}
