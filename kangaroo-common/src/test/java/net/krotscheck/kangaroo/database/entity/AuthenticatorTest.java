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
 *
 */

package net.krotscheck.kangaroo.database.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import net.krotscheck.kangaroo.database.entity.Authenticator.Deserializer;
import net.krotscheck.kangaroo.database.util.JacksonUtil;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for the authenticator entity.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticatorTest {

    /**
     * Test getting/setting the application.
     */
    @Test
    public void testGetSetClient() {
        Authenticator auth = new Authenticator();
        Client client = new Client();

        Assert.assertNull(auth.getClient());
        auth.setClient(client);
        Assert.assertEquals(client, auth.getClient());
    }

    /**
     * Test the type setter.
     */
    @Test
    public void testGetSetType() {
        Authenticator auth = new Authenticator();

        Assert.assertNull(auth.getType());
        auth.setType("foo");
        Assert.assertEquals("foo", auth.getType());
    }

    /**
     * Test getting/setting the configuration. \
     */
    @Test
    public void testGetSetConfiguration() {
        Authenticator auth = new Authenticator();
        Map<String, String> config = new HashMap<>();

        Assert.assertEquals(0, auth.getConfiguration().size());
        auth.setConfiguration(config);
        Assert.assertEquals(config, auth.getConfiguration());
    }

    /**
     * Test get/set states list.
     */
    @Test
    public void testGetSetStates() {
        Authenticator a = new Authenticator();
        List<AuthenticatorState> states = new ArrayList<>();
        states.add(new AuthenticatorState());

        Assert.assertNull(a.getStates());
        a.setStates(states);
        Assert.assertEquals(states, a.getStates());
        Assert.assertNotSame(states, a.getStates());
    }

    /**
     * Test get/set identities list.
     */
    @Test
    public void testGetSetIdentities() {
        Authenticator a = new Authenticator();
        List<UserIdentity> identities = new ArrayList<>();
        identities.add(new UserIdentity());

        Assert.assertNull(a.getIdentities());
        a.setIdentities(identities);
        Assert.assertEquals(identities, a.getIdentities());
        Assert.assertNotSame(identities, a.getIdentities());
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
        Client client = new Client();
        client.setId(UUID.randomUUID());

        List<UserIdentity> identities = new ArrayList<>();
        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identities.add(identity);

        List<AuthenticatorState> states = new ArrayList<>();
        AuthenticatorState state = new AuthenticatorState();
        state.setId(UUID.randomUUID());
        states.add(state);

        Map<String, String> config = new HashMap<>();
        config.put("one", "value");
        config.put("two", "value");

        Authenticator a = new Authenticator();
        a.setId(UUID.randomUUID());
        a.setClient(client);
        a.setCreatedDate(Calendar.getInstance());
        a.setModifiedDate(Calendar.getInstance());
        a.setType("type");
        a.setConfiguration(config);

        // These should not show up in deserialization
        a.setIdentities(identities);
        a.setStates(states);

        // De/serialize to json.
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(a);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                a.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(a.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(a.getCreatedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                a.getClient().getId().toString(),
                node.get("client").asText());
        Assert.assertEquals(
                a.getType(),
                node.get("type").asText());

        // Get the configuration node.
        JsonNode configNode = node.get("configuration");
        Assert.assertEquals(
                "value",
                configNode.get("one").asText());
        Assert.assertEquals(
                "value",
                configNode.get("two").asText());

        Assert.assertFalse(node.has("identities"));
        Assert.assertFalse(node.has("states"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(6, names.size());
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
        node.put("type", "type");

        ObjectNode configNode = m.createObjectNode();
        configNode.put("one", "value");
        configNode.put("two", "value");
        node.set("configuration", configNode);

        String output = m.writeValueAsString(node);
        Authenticator a = m.readValue(output, Authenticator.class);

        Assert.assertEquals(
                a.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(a.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(a.getModifiedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                a.getType(),
                node.get("type").asText());

        Map<String, String> config = a.getConfiguration();

        Assert.assertEquals(
                config.get("one"),
                configNode.get("one").asText());
        Assert.assertEquals(
                config.get("two"),
                configNode.get("two").asText());
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
        Authenticator u = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, u.getId());
    }
}
