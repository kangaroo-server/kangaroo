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
     * Assert that this entity can be serialized into a JSON object, and doesn't
     * carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {
        Client c = new Client();
        Application a = new Application();
        a.setId((long) 100);
        String path = "https://example.com/oauth/foo?lol=cat#omg";
        URL url = new URL(path);

        c.setApplication(a);
        c.setId((long) 100);
        c.setCreatedDate(new Date());
        c.setModifiedDate(new Date());
        c.setName("name");
        c.setRedirect(url);
        c.setReferrer(url);
        c.setClientId("clientId");

        // De/serialize to json.
        ObjectMapper m = new ObjectMapper();
        String output = m.writeValueAsString(c);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                (long) c.getId(),
                node.get("id").asLong());
        Assert.assertEquals(
                c.getCreatedDate().getTime(),
                node.get("createdDate").asLong());
        Assert.assertEquals(
                c.getModifiedDate().getTime(),
                node.get("modifiedDate").asLong());

        Assert.assertEquals(
                (long) c.getApplication().getId(),
                node.get("application").asLong());
        Assert.assertEquals(
                c.getName(),
                node.get("name").asText());
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

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(10, names.size());
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
        node.put("redirect", "https://example.com/oauth/foo?lol=cat#omg");
        node.put("referrer", "https://example.com/oauth/foo?lol=cat#omg");
        node.put("clientId", "clientId");
        node.put("authTokenExpire", 100);
        node.put("refreshTokenExpire", 100);

        String output = m.writeValueAsString(node);
        Client c = m.readValue(output, Client.class);

        Assert.assertEquals(
                (long) c.getId(),
                node.get("id").asLong());
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
        String id = String.format("\"%s\"", 1);
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        Client c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals((long) 1, (long) c.getId());
    }
}
