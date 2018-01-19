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
import net.krotscheck.kangaroo.authz.common.database.entity.Application.Deserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.mockito.Mockito.mock;

/**
 * Test the application entity.
 *
 * @author Michael Krotscheck
 */
public final class ApplicationTest {

    /**
     * Test getting/setting the owner.
     */
    @Test
    public void testGetSetOwner() {
        Application application = new Application();
        User user = new User();

        Assert.assertNull(application.getOwner());
        application.setOwner(user);
        Assert.assertEquals(user, application.getOwner());
    }

    /**
     * Test getting/setting the default role.
     */
    @Test
    public void testGetSetDefaultRole() {
        Application application = new Application();
        Role role = new Role();

        Assert.assertNull(application.getDefaultRole());
        application.setDefaultRole(role);
        Assert.assertEquals(role, application.getDefaultRole());
    }

    /**
     * Test get/set name.
     */
    @Test
    public void testGetSetName() {
        Application a = new Application();

        Assert.assertNull(a.getName());
        a.setName("foo");
        Assert.assertEquals("foo", a.getName());
    }

    /**
     * Test get/set description.
     */
    @Test
    public void testGetSetDescription() {
        Application a = new Application();

        Assert.assertNull(a.getDescription());
        a.setDescription("The application description");
        Assert.assertEquals("The application description",
                a.getDescription());
    }

    /**
     * Test get/set user list.
     */
    @Test
    public void testGetSetUsers() {
        Application a = new Application();
        List<User> users = new ArrayList<>();
        users.add(new User());

        Assert.assertEquals(0, a.getUsers().size());
        a.setUsers(users);
        Assert.assertEquals(users, a.getUsers());
        Assert.assertNotSame(users, a.getUsers());
    }

    /**
     * Test get/set client list.
     */
    @Test
    public void testGetSetClients() {
        Application a = new Application();
        List<Client> clients = new ArrayList<>();
        clients.add(new Client());

        Assert.assertEquals(0, a.getClients().size());
        a.setClients(clients);
        Assert.assertEquals(clients, a.getClients());
        Assert.assertNotSame(clients, a.getClients());
    }

    /**
     * Test get/set roles list.
     */
    @Test
    public void testGetSetRoles() {
        Application a = new Application();
        List<Role> roles = new ArrayList<>();
        roles.add(new Role());

        Assert.assertEquals(0, a.getRoles().size());
        a.setRoles(roles);
        Assert.assertEquals(roles, a.getRoles());
        Assert.assertNotSame(roles, a.getRoles());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetScopes() {
        Application a = new Application();
        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        scopes.put("foo", new ApplicationScope());

        Assert.assertEquals(0, a.getScopes().size());
        a.setScopes(scopes);
        Assert.assertEquals(scopes, a.getScopes());
        Assert.assertNotSame(scopes, a.getScopes());
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and
     * doesn't
     * carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {

        User owner = new User();
        owner.setId(IdUtil.next());

        Role defaultRole = new Role();
        defaultRole.setId(IdUtil.next());

        List<User> users = new ArrayList<>();
        User user = new User();
        user.setId(IdUtil.next());
        users.add(user);

        List<Client> clients = new ArrayList<>();
        Client client = new Client();
        client.setId(IdUtil.next());
        clients.add(client);

        List<Role> roles = new ArrayList<>();
        Role role = new Role();
        role.setId(IdUtil.next());
        roles.add(role);

        Application a = new Application();
        a.setId(IdUtil.next());
        a.setCreatedDate(Calendar.getInstance());
        a.setModifiedDate(Calendar.getInstance());
        a.setOwner(owner);
        a.setName("name");
        a.setDescription("description");

        // These four should not show up in the deserialized version.
        a.setDefaultRole(defaultRole);
        a.setClients(clients);
        a.setRoles(roles);
        a.setUsers(users);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(a);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                IdUtil.toString(a.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                a.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                a.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());
        Assert.assertEquals(
                IdUtil.toString(a.getOwner().getId()),
                node.get("owner").asText());
        Assert.assertEquals(
                IdUtil.toString(a.getDefaultRole().getId()),
                node.get("defaultRole").asText());
        Assert.assertEquals(
                a.getName(),
                node.get("name").asText());
        Assert.assertEquals(
                a.getDescription(),
                node.get("description").asText());
        Assert.assertFalse(node.has("clients"));
        Assert.assertFalse(node.has("roles"));
        Assert.assertFalse(node.has("users"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(7, names.size());
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
        node.put("description", "description");
        node.put("owner", IdUtil.toString(IdUtil.next()));

        String output = m.writeValueAsString(node);
        Application a = m.readValue(output, Application.class);

        Assert.assertEquals(
                IdUtil.toString(a.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                a.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                a.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());
        Assert.assertEquals(
                a.getName(),
                node.get("name").asText());
        Assert.assertEquals(
                a.getDescription(),
                node.get("description").asText());
        Assert.assertEquals(
                IdUtil.toString(a.getOwner().getId()),
                node.get("owner").asText());
    }

    /**
     * Test the application deserializer.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDeserializeSimple() throws Exception {
        BigInteger newInteger = IdUtil.next();
        String id = String.format("\"%s\"", IdUtil.toString(newInteger));
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        Application a = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(newInteger, a.getId());
    }
}
