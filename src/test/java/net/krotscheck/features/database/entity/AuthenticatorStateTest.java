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
import net.krotscheck.features.database.entity.AuthenticatorState.Deserializer;
import net.krotscheck.test.JacksonUtil;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for the AuthenticatorState entity.
 *
 * @author Michael Krotscheck
 */
public final class AuthenticatorStateTest {

    /**
     * Assert that we can get and set the authenticator.
     */
    @Test
    public void testGetSetAuthenticator() {
        AuthenticatorState token = new AuthenticatorState();
        Authenticator authenticator = new Authenticator();

        Assert.assertNull(token.getAuthenticator());
        token.setAuthenticator(authenticator);
        Assert.assertEquals(authenticator, token.getAuthenticator());
    }

    /**
     * Assert that we can get and set client.
     */
    @Test
    public void testGetSetClient() {
        AuthenticatorState token = new AuthenticatorState();
        Client client = new Client();

        Assert.assertNull(token.getClient());
        token.setClient(client);
        Assert.assertEquals(client, token.getClient());
    }

    /**
     * Assert that we can get and set authenticator state.
     */
    @Test
    public void testGetSetAuthenticatorState() {
        AuthenticatorState state = new AuthenticatorState();

        // Default
        Assert.assertNull(state.getAuthenticatorState());
        state.setAuthenticatorState("state");
        Assert.assertEquals("state", state.getAuthenticatorState());
    }

    /**
     * Assert that we can get and set authenticator nonce.
     */
    @Test
    public void testGetSetAuthenticatorNonce() {
        AuthenticatorState state = new AuthenticatorState();

        // Default
        Assert.assertNull(state.getAuthenticatorNonce());
        state.setAuthenticatorNonce("nonce");
        Assert.assertEquals("nonce", state.getAuthenticatorNonce());
    }

    /**
     * Assert that we can get and set client state.
     */
    @Test
    public void testGetSetClientState() {
        AuthenticatorState state = new AuthenticatorState();

        // Default
        Assert.assertNull(state.getClientState());
        state.setClientState("state");
        Assert.assertEquals("state", state.getClientState());
    }

    /**
     * Assert that we can get and set client nonce.
     */
    @Test
    public void testGetSetClientNonce() {
        AuthenticatorState state = new AuthenticatorState();

        // Default
        Assert.assertNull(state.getClientNonce());
        state.setClientNonce("nonce");
        Assert.assertEquals("nonce", state.getClientNonce());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetScopes() {
        AuthenticatorState state = new AuthenticatorState();
        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        scopes.put("test", new ApplicationScope());

        Assert.assertNull(state.getClientScope());
        state.setClientScope(scopes);
        Assert.assertEquals(scopes, state.getClientScope());
        Assert.assertNotSame(scopes, state.getClientScope());
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
        Authenticator authenticator = new Authenticator();
        authenticator.setId(UUID.randomUUID());

        Client client = new Client();
        client.setId(UUID.randomUUID());

        AuthenticatorState state = new AuthenticatorState();
        state.setId(UUID.randomUUID());
        state.setCreatedDate(Calendar.getInstance());
        state.setModifiedDate(Calendar.getInstance());
        state.setAuthenticator(authenticator);
        state.setClient(client);
        state.setAuthenticatorState("authenticatorState");
        state.setAuthenticatorNonce("authenticatorNonce");
        state.setClientState("clientState");
        state.setClientNonce("clientNonce");

        // De/serialize to json.
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(state);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                state.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(state.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(state.getCreatedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                state.getAuthenticator().getId().toString(),
                node.get("authenticator").asText());
        Assert.assertEquals(
                state.getClient().getId().toString(),
                node.get("client").asText());
        Assert.assertEquals(
                state.getAuthenticatorState(),
                node.get("authenticatorState").asText());
        Assert.assertEquals(
                state.getAuthenticatorNonce(),
                node.get("authenticatorNonce").asText());
        Assert.assertEquals(
                state.getClientState(),
                node.get("clientState").asText());
        Assert.assertEquals(
                state.getClientNonce(),
                node.get("clientNonce").asText());

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(9, names.size());
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
        node.put("authenticatorState", "authenticatorState");
        node.put("authenticatorNonce", "authenticatorNonce");
        node.put("clientState", "clientState");
        node.put("clientNonce", "clientNonce");
        node.put("clientScope", "clientScope");

        String output = m.writeValueAsString(node);
        AuthenticatorState c = m.readValue(output, AuthenticatorState.class);

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
                c.getAuthenticatorState(),
                node.get("authenticatorState").asText());
        Assert.assertEquals(
                c.getAuthenticatorNonce(),
                node.get("authenticatorNonce").asText());
        Assert.assertEquals(
                c.getClientState(),
                node.get("clientState").asText());
        Assert.assertEquals(
                c.getClientNonce(),
                node.get("clientNonce").asText());
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
        AuthenticatorState c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, c.getId());
    }
}
