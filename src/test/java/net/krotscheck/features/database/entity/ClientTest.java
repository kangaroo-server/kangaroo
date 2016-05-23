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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.krotscheck.features.database.entity.Client.Deserializer;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
     * Assert that we can get and set the secret.
     */
    @Test
    public void testGetSetSecret() {
        Client c = new Client();
        String secret = "secret";

        Assert.assertNull(c.getClientSecret());
        c.setClientSecret(secret);
        Assert.assertEquals(secret, c.getClientSecret());
    }

    /**
     * Assert that we can get and set the type.
     */
    @Test
    public void testGetSetType() {
        Client c = new Client();

        Assert.assertEquals(ClientType.AuthorizationGrant, c.getType());
        c.setType(ClientType.Implicit);
        Assert.assertEquals(ClientType.Implicit, c.getType());
    }

    /**
     * Assert that the referral URI may be get and set.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetSetReferrers() throws Exception {
        Client c = new Client();

        Set<URI> referrers = new HashSet<>();
        URI referrer = new URI("https://example.com/oauth/foo?lol=cat#omg");
        referrers.add(referrer);

        Assert.assertNull(c.getReferrers());
        c.setReferrers(referrers);
        Assert.assertEquals(referrers, c.getReferrers());
        Assert.assertTrue(c.getReferrers().contains(referrer));
    }

    /**
     * Assert that we can set and get the redirection URIs.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetSetRedirects() throws Exception {
        Client c = new Client();

        Set<URI> redirects = new HashSet<>();
        URI redirect = new URI("https://example.com/oauth/foo?lol=cat#omg");
        redirects.add(redirect);

        Assert.assertNull(c.getRedirects());
        c.setRedirects(redirects);
        Assert.assertEquals(redirects, c.getRedirects());
        Assert.assertTrue(c.getRedirects().contains(redirect));
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
     * Test get/set authenticator list.
     */
    @Test
    public void testGetSetAuthenticators() {
        Client client = new Client();
        List<Authenticator> authenticators = new ArrayList<>();
        authenticators.add(new Authenticator());

        Assert.assertNull(client.getAuthenticators());
        client.setAuthenticators(authenticators);
        Assert.assertEquals(authenticators, client.getAuthenticators());
        Assert.assertNotSame(authenticators, client.getAuthenticators());
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

        Set<URI> referrers = new HashSet<>();
        referrers.add(new URI("https://example.com/oauth/foo?lol=cat#omg"));
        Set<URI> redirects = new HashSet<>();
        redirects.add(new URI("https://example.com/oauth/foo?lol=cat#omg"));

        Client c = new Client();
        c.setApplication(application);
        c.setId(UUID.randomUUID());
        c.setCreatedDate(new Date());
        c.setModifiedDate(new Date());
        c.setName("name");
        c.setClientSecret("clientSecret");
        c.setType(ClientType.AuthorizationGrant);
        c.setRedirects(redirects);
        c.setReferrers(referrers);

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
                c.getClientSecret(),
                node.get("clientSecret").asText());
        Assert.assertEquals(
                c.getType().toString(),
                node.get("type").asText());
        Assert.assertFalse(node.has("tokens"));
        Assert.assertFalse(node.has("states"));

        // Extract the referrers
        ArrayNode referrerNode = (ArrayNode) node.get("referrers");
        Assert.assertEquals("https://example.com/oauth/foo?lol=cat#omg",
                referrerNode.get(0).asText());

        // Extract the redirects
        ArrayNode redirectNode = (ArrayNode) node.get("redirects");
        Assert.assertEquals("https://example.com/oauth/foo?lol=cat#omg",
                redirectNode.get(0).asText());

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
        ObjectMapper m = new ObjectMapper();
        ObjectNode node = m.createObjectNode();
        node.put("id", UUID.randomUUID().toString());
        node.put("createdDate", new Date().getTime());
        node.put("modifiedDate", new Date().getTime());
        node.put("name", "name");
        node.put("type", "Implicit");
        node.put("clientSecret", "clientSecret");

        ArrayNode referrers = node.arrayNode();
        referrers.add("https://example.com/oauth/foo?lol=cat#omg");
        node.set("referrers", referrers);

        ArrayNode redirects = node.arrayNode();
        redirects.add("https://example.com/oauth/foo?lol=cat#omg");
        node.set("redirects", redirects);

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
                c.getClientSecret(),
                node.get("clientSecret").asText());
        Assert.assertEquals(
                c.getType().toString(),
                node.get("type").asText());
        Assert.assertEquals(1, c.getRedirects().size());
        Assert.assertEquals(1, c.getReferrers().size());
        Assert.assertTrue(c.getRedirects()
                .contains(new URI("https://example"
                        + ".com/oauth/foo?lol=cat#omg")));
        Assert.assertTrue(c.getReferrers()
                .contains(new URI("https://example"
                        + ".com/oauth/foo?lol=cat#omg")));
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
