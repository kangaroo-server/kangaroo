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
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator.Deserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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

        assertNull(auth.getClient());
        auth.setClient(client);
        assertEquals(client, auth.getClient());
    }

    /**
     * Test the type setter.
     */
    @Test
    public void testGetSetType() {
        Authenticator auth = new Authenticator();

        assertNull(auth.getType());
        auth.setType(AuthenticatorType.Password);
        assertEquals(AuthenticatorType.Password, auth.getType());
    }

    /**
     * Test getting/setting the configuration. \
     */
    @Test
    public void testGetSetConfiguration() {
        Authenticator auth = new Authenticator();
        Map<String, String> config = new HashMap<>();

        assertEquals(0, auth.getConfiguration().size());
        auth.setConfiguration(config);
        assertEquals(config, auth.getConfiguration());
    }

    /**
     * Test get/set states list.
     */
    @Test
    public void testGetSetStates() {
        Authenticator a = new Authenticator();
        List<AuthenticatorState> states = new ArrayList<>();
        states.add(new AuthenticatorState());

        assertEquals(0, a.getStates().size());
        a.setStates(states);
        assertEquals(states, a.getStates());
        assertNotSame(states, a.getStates());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    public void testGetOwner() {
        Authenticator authenticator = new Authenticator();
        Client spy = spy(new Client());

        // Null check
        assertNull(authenticator.getOwner());

        authenticator.setClient(spy);
        authenticator.getOwner();

        Mockito.verify(spy).getOwner();
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
        client.setId(IdUtil.next());

        List<UserIdentity> identities = new ArrayList<>();
        UserIdentity identity = new UserIdentity();
        identity.setId(IdUtil.next());
        identities.add(identity);

        List<AuthenticatorState> states = new ArrayList<>();
        AuthenticatorState state = new AuthenticatorState();
        state.setId(IdUtil.next());
        states.add(state);

        Map<String, String> config = new HashMap<>();
        config.put("one", "value");
        config.put("two", "value");

        Authenticator a = new Authenticator();
        a.setId(IdUtil.next());
        a.setClient(client);
        a.setCreatedDate(Calendar.getInstance());
        a.setModifiedDate(Calendar.getInstance());
        a.setType(AuthenticatorType.Test);
        a.setConfiguration(config);

        // These should not show up in deserialization
        a.setStates(states);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(a);
        JsonNode node = m.readTree(output);

        assertEquals(
                IdUtil.toString(a.getId()),
                node.get("id").asText());
        assertEquals(
                a.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        assertEquals(
                a.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        assertEquals(
                IdUtil.toString(a.getClient().getId()),
                node.get("client").asText());
        assertEquals(
                a.getType().toString(),
                node.get("type").asText());

        // Get the configuration node.
        JsonNode configNode = node.get("configuration");
        assertEquals(
                "value",
                configNode.get("one").asText());
        assertEquals(
                "value",
                configNode.get("two").asText());

        assertFalse(node.has("identities"));
        assertFalse(node.has("states"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        assertEquals(6, names.size());
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
        node.put("type", AuthenticatorType.Test.toString());

        ObjectNode configNode = m.createObjectNode();
        configNode.put("one", "value");
        configNode.put("two", "value");
        node.set("configuration", configNode);

        String output = m.writeValueAsString(node);
        Authenticator a = m.readValue(output, Authenticator.class);

        assertEquals(
                IdUtil.toString(a.getId()),
                node.get("id").asText());
        assertEquals(
                a.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        assertEquals(
                a.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        assertEquals(
                a.getType().toString(),
                node.get("type").asText());
        assertNull(a.getClient());

        Map<String, String> config = a.getConfiguration();

        assertEquals(
                config.get("one"),
                configNode.get("one").asText());
        assertEquals(
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
        BigInteger newId = IdUtil.next();
        String id = String.format("\"%s\"", IdUtil.toString(newId));
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        Authenticator u = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        assertEquals(newId, u.getId());
    }
}
