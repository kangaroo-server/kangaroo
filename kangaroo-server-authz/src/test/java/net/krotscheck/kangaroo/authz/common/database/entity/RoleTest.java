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

package net.krotscheck.kangaroo.authz.common.database.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.krotscheck.kangaroo.authz.common.database.entity.Role.Deserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Unit test for the role data model.
 *
 * @author Michael Krotscheck
 */
public final class RoleTest {

    /**
     * Assert that we can get and set the application to which this role
     * belongs.
     */
    @Test
    public void testGetSetApplication() {
        Role c = new Role();
        Application a = new Application();

        Assert.assertNull(c.getApplication());
        c.setApplication(a);
        Assert.assertEquals(a, c.getApplication());
    }

    /**
     * Assert that we can get and set the name.
     */
    @Test
    public void testGetSetName() {
        Role role = new Role();
        String name = "test";

        Assert.assertNull(role.getName());
        role.setName(name);
        Assert.assertEquals(name, role.getName());
    }

    /**
     * Test get/set user list.
     */
    @Test
    public void testGetSetUsers() {
        Role role = new Role();
        List<User> users = new ArrayList<>();
        users.add(new User());

        Assert.assertEquals(0, role.getUsers().size());
        role.setUsers(users);
        Assert.assertEquals(users, role.getUsers());
        Assert.assertNotSame(users, role.getUsers());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetScopes() {
        Role role = new Role();
        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        scopes.put("foo", new ApplicationScope());

        Assert.assertEquals(0, role.getScopes().size());
        role.setScopes(scopes);
        Assert.assertEquals(scopes, role.getScopes());
        Assert.assertNotSame(scopes, role.getScopes());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    public void testGetOwner() {
        Role role = new Role();
        Application spy = spy(new Application());

        // Null check
        Assert.assertNull(role.getOwner());

        role.setApplication(spy);
        role.getOwner();

        Mockito.verify(spy).getOwner();
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and
     * doesn't carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setId(IdUtil.next());
        users.add(user);

        Application a = new Application();
        a.setId(IdUtil.next());

        Role role = new Role();
        role.setApplication(a);
        role.setId(IdUtil.next());
        role.setCreatedDate(Calendar.getInstance());
        role.setModifiedDate(Calendar.getInstance());
        role.setName("name");
        role.setUsers(users);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(role);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                IdUtil.toString(role.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                role.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                role.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        Assert.assertEquals(
                IdUtil.toString(role.getApplication().getId()),
                node.get("application").asText());
        Assert.assertEquals(
                role.getName(),
                node.get("name").asText());

        // Should not be serialized.
        Assert.assertFalse(node.has("users"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(5, names.size());
    }

    /**
     * Assert that this entity can be deserialized from a JSON object.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonDeserializable() throws Exception {
        ObjectMapper m = new ObjectMapperFactory().get();
        long timestamp = Calendar.getInstance().getTimeInMillis() / 1000;
        ObjectNode node = m.createObjectNode();
        node.put("id", IdUtil.toString(IdUtil.next()));
        node.put("createdDate", timestamp);
        node.put("modifiedDate", timestamp);
        node.put("name", "name");
        node.put("application", IdUtil.toString(IdUtil.next()));

        String output = m.writeValueAsString(node);
        Role c = m.readValue(output, Role.class);

        Assert.assertEquals(
                IdUtil.toString(c.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                c.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                c.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        Assert.assertEquals(
                c.getName(),
                node.get("name").asText());
        Assert.assertEquals(
                IdUtil.toString(c.getApplication().getId()),
                node.get("application").asText());
    }

    /**
     * Test the deserializer.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDeserializeSimple() throws Exception {
        BigInteger newId = IdUtil.next();
        String id = String.format("\"%s\"", IdUtil.toString(newId));
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        Role c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(newId, c.getId());
    }
}
