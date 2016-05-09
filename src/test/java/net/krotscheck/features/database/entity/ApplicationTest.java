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

import static org.mockito.Mockito.mock;

/**
 * Test the application entity.
 *
 * @author Michael Krotscheck
 */
public final class ApplicationTest {

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
     * Test get/set user record.
     */
    @Test
    public void testGetSetApplications() {
        Application a = new Application();
        User u = new User();

        Assert.assertNull(a.getUser());
        a.setUser(u);
        Assert.assertEquals(u, a.getUser());
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
     * Test the application deserializer.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDeserializeSimple() throws Exception {
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser("1");
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        Application a = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals((long) 1, (long) a.getId());
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and doesn't
     * carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {
        Client c = new Client();
        User u = new User();
        u.setId((long) 199);
        List<Client> clients = new ArrayList<>();
        Client one = new Client();
        one.setId((long) 1);
        Client two = new Client();
        two.setId((long) 2);
        clients.add(one);
        clients.add(two);

        Application a = new Application();
        a.setId((long) 100);
        a.setCreatedDate(new Date());
        a.setModifiedDate(new Date());

        a.setUser(u);
        a.setClients(clients);
        a.setName("name");

        // De/serialize to json.
        ObjectMapper m = new ObjectMapper();
        String output = m.writeValueAsString(a);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                (long) a.getId(),
                node.get("id").asLong());
        Assert.assertEquals(
                a.getCreatedDate().getTime(),
                node.get("createdDate").asLong());
        Assert.assertEquals(
                a.getModifiedDate().getTime(),
                node.get("modifiedDate").asLong());
        Assert.assertEquals(
                (long) a.getUser().getId(),
                node.get("user").asLong());
        Assert.assertEquals(
                a.getName(),
                node.get("name").asText());
        Assert.assertFalse(node.has("clients"));

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
        node.put("id", 100);
        node.put("createdDate", new Date().getTime());
        node.put("modifiedDate", new Date().getTime());
        node.put("name", "name");

        String output = m.writeValueAsString(node);
        Application a = m.readValue(output, Application.class);

        Assert.assertEquals(
                (long) a.getId(),
                node.get("id").asLong());
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
}
