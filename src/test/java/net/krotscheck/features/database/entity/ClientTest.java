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
import net.krotscheck.features.database.entity.Client.Deserializer;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Unit test for the client data model.
 *
 * @author Michael Krotscheck
 */
public final class ClientTest {

    /**
     * Assert that we can get and set the application to which this client
     * belongs.
     */
    @Test
    public void testGetSetApplication() {
        Client c = new Client();
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
        Client c = new Client();
        String name = "test";

        Assert.assertNull(c.getName());
        c.setName(name);
        Assert.assertEquals(name, c.getName());
    }

    /**
     * Assert that we can get and set the type.
     */
    @Test
    public void testGetSetType() {
        Client c = new Client();

        Assert.assertEquals(ClientType.Public, c.getType());
        c.setType(ClientType.Confidential);
        Assert.assertEquals(ClientType.Confidential, c.getType());
    }

    /**
     * Assert that we can get and set the client ID.
     */
    @Test
    public void testGetSetClientId() {
        Client c = new Client();
        String clientId = "test";

        Assert.assertNull(c.getClientId());
        c.setClientId(clientId);
        Assert.assertEquals(clientId, c.getClientId());
    }

    /**
     * Assert that the referral URL may be get and set.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetSetReferrer() throws Exception {
        Client c = new Client();
        String url = "https://example.com/oauth/foo?lol=cat#omg";
        URL referrer = new URL(url);

        Assert.assertNull(c.getReferrer());
        c.setReferrer(referrer);
        Assert.assertEquals(referrer, c.getReferrer());
        Assert.assertEquals(url, c.getReferrer().toString());
    }

    /**
     * Assert that we can set and get the redirection URL.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetSetRedirect() throws Exception {
        Client c = new Client();
        String url = "https://example.com/oauth/foo?lol=cat#omg";
        URL redirect = new URL(url);

        Assert.assertNull(c.getRedirect());
        c.setRedirect(redirect);
        Assert.assertEquals(redirect, c.getRedirect());
        Assert.assertEquals(url, c.getRedirect().toString());
    }

    /**
     * Test that the authtoken expiration time may be set and get.
     */
    @Test
    public void testGetSetAuthTokenExpire() {
        Client c = new Client();
        Integer expire = 1000;

        Assert.assertEquals(3600, (int) c.getAuthTokenExpire()); // Default
        c.setAuthTokenExpire(expire);
        Assert.assertEquals(expire, c.getAuthTokenExpire());
    }

    /**
     * Test that the refreshToken expiration time may be set and get.
     */
    @Test
    public void testGetSetRefreshTokenExpire() {
        Client c = new Client();
        Integer expire = 1000;

        Assert.assertEquals(604800, (int) c.getRefreshTokenExpire()); // Default
        c.setRefreshTokenExpire(expire);
        Assert.assertEquals(expire, c.getRefreshTokenExpire());
    }

    /**
     * Test get/set states list.
     */
    @Test
    public void testGetSetStates() {
        Client client = new Client();
        List<AuthenticatorState> states = new ArrayList<>();
        states.add(new AuthenticatorState());

        Assert.assertNull(client.getStates());
        client.setStates(states);
        Assert.assertEquals(states, client.getStates());
        Assert.assertNotSame(states, client.getStates());
    }

    /**
     * Test get/set tokens list.
     */
    @Test
    public void testGetSetTokens() {
        Client client = new Client();
        List<OAuthToken> tokens = new ArrayList<>();
        tokens.add(new OAuthToken());

        Assert.assertNull(client.getTokens());
        client.setTokens(tokens);
        Assert.assertEquals(tokens, client.getTokens());
        Assert.assertNotSame(tokens, client.getTokens());
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and doesn't
     * carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {
        Application application = new Application();
        application.setId(UUID.randomUUID());

        List<OAuthToken> tokens = new ArrayList<>();
        OAuthToken token = new OAuthToken();
        token.setId(UUID.randomUUID());
        tokens.add(token);

        List<AuthenticatorState> states = new ArrayList<>();
        AuthenticatorState state = new AuthenticatorState();
        state.setId(UUID.randomUUID());
        states.add(state);

        String path = "https://example.com/oauth/foo?lol=cat#omg";
        URL url = new URL(path);

        Client c = new Client();
        c.setApplication(application);
        c.setId(UUID.randomUUID());
        c.setCreatedDate(new Date());
        c.setModifiedDate(new Date());
        c.setName("name");
        c.setType(ClientType.Confidential);
        c.setRedirect(url);
        c.setReferrer(url);
        c.setClientId("clientId");

        // These should not serialize.
        c.setTokens(tokens);
        c.setStates(states);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapper();
        String output = m.writeValueAsString(c);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                c.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                c.getCreatedDate().getTime(),
                node.get("createdDate").asLong());
        Assert.assertEquals(
                c.getModifiedDate().getTime(),
                node.get("modifiedDate").asLong());

        Assert.assertEquals(
                c.getApplication().getId().toString(),
                node.get("application").asText());
        Assert.assertEquals(
                c.getName(),
                node.get("name").asText());
        Assert.assertEquals(
                c.getType().toString(),
                node.get("type").asText());
        Assert.assertEquals(
                c.getRedirect().toString(),
                node.get("redirect").asText());
        Assert.assertEquals(
                c.getReferrer().toString(),
                node.get("referrer").asText());
        Assert.assertEquals(
                c.getClientId(),
                node.get("clientId").asText());
        Assert.assertEquals(
                (long) c.getAuthTokenExpire(),
                node.get("authTokenExpire").asLong());
        Assert.assertEquals(
                (long) c.getRefreshTokenExpire(),
                node.get("refreshTokenExpire").asLong());

        Assert.assertFalse(node.has("tokens"));
        Assert.assertFalse(node.has("states"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(11, names.size());
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
        node.put("type", "Confidential");
        node.put("redirect", "https://example.com/oauth/foo?lol=cat#omg");
        node.put("referrer", "https://example.com/oauth/foo?lol=cat#omg");
        node.put("clientId", "clientId");
        node.put("authTokenExpire", 100);
        node.put("refreshTokenExpire", 100);

        String output = m.writeValueAsString(node);
        Client c = m.readValue(output, Client.class);

        Assert.assertEquals(
                c.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                c.getCreatedDate().getTime(),
                node.get("createdDate").asLong());
        Assert.assertEquals(
                c.getModifiedDate().getTime(),
                node.get("modifiedDate").asLong());

        Assert.assertEquals(
                c.getName(),
                node.get("name").asText());
        Assert.assertEquals(
                c.getType().toString(),
                node.get("type").asText());
        Assert.assertEquals(
                c.getRedirect().toString(),
                node.get("redirect").asText());
        Assert.assertEquals(
                c.getReferrer().toString(),
                node.get("referrer").asText());
        Assert.assertEquals(
                c.getClientId(),
                node.get("clientId").asText());
        Assert.assertEquals(
                (long) c.getAuthTokenExpire(),
                node.get("authTokenExpire").asLong());
        Assert.assertEquals(
                (long) c.getRefreshTokenExpire(),
                node.get("refreshTokenExpire").asLong());
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
        Client c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, c.getId());
    }
}
