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
import net.krotscheck.features.database.entity.Application.Deserializer;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
     * Test get/set user list.
     */
    @Test
    public void testGetSetUsers() {
        Application a = new Application();
        List<User> users = new ArrayList<>();
        users.add(new User());

        Assert.assertNull(a.getUsers());
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

        Assert.assertNull(a.getClients());
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

        Assert.assertNull(a.getRoles());
        a.setRoles(roles);
        Assert.assertEquals(roles, a.getRoles());
        Assert.assertNotSame(roles, a.getRoles());
    }

    /**
     * Test get/set authenticator list.
     */
    @Test
    public void testGetSetAuthenticators() {
        Application a = new Application();
        List<Authenticator> authenticators = new ArrayList<>();
        authenticators.add(new Authenticator());

        Assert.assertNull(a.getAuthenticators());
        a.setAuthenticators(authenticators);
        Assert.assertEquals(authenticators, a.getAuthenticators());
        Assert.assertNotSame(authenticators, a.getAuthenticators());
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and doesn't
     * carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {

        User owner = new User();
        owner.setId(UUID.randomUUID());

        List<User> users = new ArrayList<>();
        User user = new User();
        user.setId(UUID.randomUUID());
        users.add(user);

        List<Client> clients = new ArrayList<>();
        Client client = new Client();
        client.setId(UUID.randomUUID());
        clients.add(client);

        List<Role> roles = new ArrayList<>();
        Role role = new Role();
        role.setId(UUID.randomUUID());
        roles.add(role);

        List<Authenticator> authenticators = new ArrayList<>();
        Authenticator authenticator = new Authenticator();
        authenticator.setId(UUID.randomUUID());
        authenticators.add(authenticator);

        Application a = new Application();
        a.setId(UUID.randomUUID());
        a.setCreatedDate(new Date());
        a.setModifiedDate(new Date());
        a.setOwner(owner);
        a.setName("name");

        // These four should not show up in the deserialized version.
        a.setClients(clients);
        a.setRoles(roles);
        a.setUsers(users);
        a.setAuthenticators(authenticators);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapper();
        String output = m.writeValueAsString(a);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                a.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                a.getCreatedDate().getTime(),
                node.get("createdDate").asLong());
        Assert.assertEquals(
                a.getModifiedDate().getTime(),
                node.get("modifiedDate").asLong());
        Assert.assertEquals(
                a.getOwner().getId().toString(),
                node.get("owner").asText());
        Assert.assertEquals(
                a.getName(),
                node.get("name").asText());
        Assert.assertFalse(node.has("clients"));
        Assert.assertFalse(node.has("roles"));
        Assert.assertFalse(node.has("authenticators"));
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
        ObjectMapper m = new ObjectMapper();
        ObjectNode node = m.createObjectNode();
        node.put("id", UUID.randomUUID().toString());
        node.put("createdDate", new Date().getTime());
        node.put("modifiedDate", new Date().getTime());
        node.put("name", "name");

        String output = m.writeValueAsString(node);
        Application a = m.readValue(output, Application.class);

        Assert.assertEquals(
                a.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                a.getCreatedDate().getTime(),
                node.get("createdDate").asLong());
        Assert.assertEquals(
                a.getModifiedDate().getTime(),
                node.get("modifiedDate").asLong());
        Assert.assertEquals(
                a.getName(),
                node.get("name").asText());
    }

    /**
     * Test the application deserializer.
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
        Application a = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, a.getId());
    }
}
