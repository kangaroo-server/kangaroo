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

package net.krotscheck.features.database.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import net.krotscheck.features.database.entity.Role.Deserializer;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import net.krotscheck.test.JacksonUtil;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

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

        Assert.assertNull(role.getUsers());
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
        List<ApplicationScope> scopes = new ArrayList<>();
        scopes.add(new ApplicationScope());

        Assert.assertNull(role.getScopes());
        role.setScopes(scopes);
        Assert.assertEquals(scopes, role.getScopes());
        Assert.assertNotSame(scopes, role.getScopes());
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
        user.setId(UUID.randomUUID());
        users.add(user);

        Application a = new Application();
        a.setId(UUID.randomUUID());

        Role role = new Role();
        role.setApplication(a);
        role.setId(UUID.randomUUID());
        role.setCreatedDate(Calendar.getInstance());
        role.setModifiedDate(Calendar.getInstance());
        role.setName("name");
        role.setUsers(users);

        // De/serialize to json.
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(role);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                role.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(role.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(role.getCreatedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                role.getApplication().getId().toString(),
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
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        ObjectNode node = m.createObjectNode();
        node.put("id", UUID.randomUUID().toString());
        node.put("createdDate",
                format.format(Calendar.getInstance().getTime()));
        node.put("modifiedDate",
                format.format(Calendar.getInstance().getTime()));
        node.put("name", "name");

        String output = m.writeValueAsString(node);
        Role c = m.readValue(output, Role.class);

        Assert.assertEquals(
                c.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(c.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(c.getModifiedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                c.getName(),
                node.get("name").asText());
    }

    /**
     * Test the deserializer.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDeserializeSimple() throws Exception {
        UUID uuid = UUID.randomUUID();
        String id = String.format("\"%s\"", uuid);
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        Role c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, c.getId());
    }
}
