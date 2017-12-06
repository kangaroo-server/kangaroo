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
import net.krotscheck.kangaroo.authz.common.database.entity.Client.Deserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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

        Assert.assertNull(c.getType());
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

        List<ClientReferrer> referrers = new ArrayList<>();
        URI referrer = new URI("https://example.com/oauth/foo?lol=cat#omg");
        ClientReferrer r = new ClientReferrer();
        r.setClient(c);
        r.setUri(referrer);
        referrers.add(r);

        Assert.assertEquals(0, c.getReferrers().size());
        c.setReferrers(referrers);
        Assert.assertEquals(referrers, c.getReferrers());
        Assert.assertTrue(c.getReferrers().contains(r));
    }

    /**
     * Assert that we can set and get the redirection URIs.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetSetRedirects() throws Exception {
        Client c = new Client();

        List<ClientRedirect> redirects = new ArrayList<>();
        URI referrer = new URI("https://example.com/oauth/foo?lol=cat#omg");
        ClientRedirect r = new ClientRedirect();
        r.setClient(c);
        r.setUri(referrer);
        redirects.add(r);

        Assert.assertEquals(0, c.getRedirects().size());
        c.setRedirects(redirects);
        Assert.assertEquals(redirects, c.getRedirects());
        Assert.assertTrue(c.getRedirects().contains(r));
    }

    /**
     * Test get/set tokens list.
     */
    @Test
    public void testGetSetTokens() {
        Client client = new Client();
        List<OAuthToken> tokens = new ArrayList<>();
        tokens.add(new OAuthToken());

        Assert.assertEquals(0, client.getTokens().size());
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

        Assert.assertEquals(0, client.getAuthenticators().size());
        client.setAuthenticators(authenticators);
        Assert.assertEquals(authenticators, client.getAuthenticators());
        Assert.assertNotSame(authenticators, client.getAuthenticators());
    }

    /**
     * Test getting/setting the configuration.
     */
    @Test
    public void testGetSetConfiguration() {
        Client client = new Client();
        Map<String, String> configuration = new HashMap<>();

        Assert.assertEquals(0, client.getConfiguration().size());
        client.setConfiguration(configuration);
        Assert.assertEquals(configuration, client.getConfiguration());
        Assert.assertNotSame(configuration, client.getConfiguration());
    }

    /**
     * Assert that we can get the default values for the access expires in
     * token.
     */
    @Test
    public void testGetAccessExpiresIn() {
        Client client = new Client();
        Map<String, String> configuration = new HashMap<>();
        Integer expectedDefault = 10 * 60; // Ten minutes

        // Assert no config.
        Assert.assertEquals(expectedDefault, client.getAccessTokenExpireIn());

        // Assert empty config.
        client.setConfiguration(configuration);
        Assert.assertEquals(expectedDefault, client.getAccessTokenExpireIn());

        // Assert unparseable config
        configuration.put(ClientConfig.ACCESS_TOKEN_EXPIRES_NAME, "not_an_int");
        client.setConfiguration(configuration);
        Assert.assertEquals(expectedDefault, client.getAccessTokenExpireIn());

        // Assert parseable config
        configuration.put(ClientConfig.ACCESS_TOKEN_EXPIRES_NAME, "50");
        client.setConfiguration(configuration);
        Assert.assertEquals((Integer) 50, client.getAccessTokenExpireIn());
    }

    /**
     * Assert that we can get the default values for the refresh token.
     */
    @Test
    public void testGetRefreshExpiresIn() {
        Client client = new Client();
        Map<String, String> configuration = new HashMap<>();
        Integer expectedDefault = 60 * 60 * 24 * 30; // One month.

        // Assert no config.
        Assert.assertEquals(expectedDefault, client.getRefreshTokenExpireIn());

        // Assert empty config.
        client.setConfiguration(configuration);
        Assert.assertEquals(expectedDefault, client.getRefreshTokenExpireIn());

        // Assert unparseable config
        configuration
                .put(ClientConfig.REFRESH_TOKEN_EXPIRES_NAME, "not_an_int");
        client.setConfiguration(configuration);
        Assert.assertEquals(expectedDefault, client.getRefreshTokenExpireIn());

        // Assert parseable config
        configuration.put(ClientConfig.REFRESH_TOKEN_EXPIRES_NAME, "50");
        client.setConfiguration(configuration);
        Assert.assertEquals((Integer) 50, client.getRefreshTokenExpireIn());
    }

    /**
     * Assert that we can get the default values for the refresh token.
     */
    @Test
    public void getGetAuthorizationCodeExpiresIn() {
        Client client = new Client();
        Map<String, String> configuration = new HashMap<>();
        Integer expectedDefault = 60 * 10; // Ten minutes.

        // Assert no config.
        Assert.assertEquals(expectedDefault,
                client.getAuthorizationCodeExpiresIn());

        // Assert empty config.
        client.setConfiguration(configuration);
        Assert.assertEquals(expectedDefault,
                client.getAuthorizationCodeExpiresIn());

        // Assert unparseable config
        configuration
                .put(ClientConfig.AUTHORIZATION_CODE_EXPIRES_NAME,
                        "not_an_int");
        client.setConfiguration(configuration);
        Assert.assertEquals(expectedDefault,
                client.getAuthorizationCodeExpiresIn());

        // Assert parseable config
        configuration.put(ClientConfig.AUTHORIZATION_CODE_EXPIRES_NAME, "50");
        client.setConfiguration(configuration);
        Assert.assertEquals((Integer) 50,
                client.getAuthorizationCodeExpiresIn());
    }

    /**
     * Assert that we retrieve the owner from the parent authenticator.
     */
    @Test
    public void testGetOwner() {
        Client client = new Client();
        Application spy = spy(new Application());

        // Null check
        Assert.assertNull(client.getOwner());

        client.setApplication(spy);
        client.getOwner();

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
        Application application = new Application();
        application.setId(IdUtil.next());

        List<OAuthToken> tokens = new ArrayList<>();
        OAuthToken token = new OAuthToken();
        token.setId(IdUtil.next());
        tokens.add(token);

        Map<String, String> configuration = new HashMap<>();
        configuration.put("one", "value");
        configuration.put("two", "value");

        Client c = new Client();
        c.setApplication(application);
        c.setId(IdUtil.next());
        c.setCreatedDate(Calendar.getInstance());
        c.setModifiedDate(Calendar.getInstance());
        c.setName("name");
        c.setClientSecret("clientSecret");
        c.setType(ClientType.AuthorizationGrant);
        c.setConfiguration(configuration);

        List<ClientRedirect> redirects = new ArrayList<>();
        ClientRedirect redirect = new ClientRedirect();
        redirect.setClient(c);
        redirect.setUri(new URI("https://example.com/oauth/foo?lol=cat#omg"));
        c.getRedirects().add(redirect);

        List<ClientReferrer> referrers = new ArrayList<>();
        ClientReferrer referrer = new ClientReferrer();
        redirect.setClient(c);
        redirect.setUri(new URI("https://example.com/oauth/foo?lol=cat#omg"));
        c.getReferrers().add(referrer);

        // These should not serialize.
        c.setTokens(tokens);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(c);
        JsonNode node = m.readTree(output);

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
                IdUtil.toString(c.getApplication().getId()),
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

        // Get the configuration node.
        JsonNode configurationNode = node.get("configuration");
        Assert.assertEquals(
                "value",
                configurationNode.get("one").asText());
        Assert.assertEquals(
                "value",
                configurationNode.get("two").asText());

        // These should not exist.
        Assert.assertFalse(node.has("referrers"));
        Assert.assertFalse(node.has("redirects"));
        Assert.assertFalse(node.has("tokens"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(8, names.size());
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
        node.put("type", "Implicit");
        node.put("clientSecret", "clientSecret");
        node.put("application", IdUtil.toString(IdUtil.next()));

        ObjectNode configurationNode = m.createObjectNode();
        configurationNode.put("one", "value");
        configurationNode.put("two", "value");
        node.set("configuration", configurationNode);

        String output = m.writeValueAsString(node);
        Client c = m.readValue(output, Client.class);

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
                c.getClientSecret(),
                node.get("clientSecret").asText());
        Assert.assertEquals(
                c.getType().toString(),
                node.get("type").asText());

        Map<String, String> configuration = c.getConfiguration();

        Assert.assertEquals(
                configuration.get("one"),
                configurationNode.get("one").asText());
        Assert.assertEquals(
                configuration.get("two"),
                configurationNode.get("two").asText());
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
        Client c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(newId, c.getId());
    }
}
