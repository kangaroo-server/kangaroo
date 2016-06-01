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
import net.krotscheck.features.database.entity.OAuthToken.Deserializer;
import net.krotscheck.features.jackson.ObjectMapperFactory;
import net.krotscheck.test.JacksonUtil;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Unit test for the OAuth Token Entity.
 *
 * @author Michael Krotscheck
 */
public final class OAuthTokenTest {

    /**
     * Assert that we can get and set the token's user identity.
     */
    @Test
    public void testGetSetIdentity() {
        OAuthToken token = new OAuthToken();
        UserIdentity identity = new UserIdentity();

        Assert.assertNull(token.getIdentity());
        token.setIdentity(identity);
        Assert.assertEquals(identity, token.getIdentity());
    }

    /**
     * Assert that we can get and set the token's client.
     */
    @Test
    public void testGetSetClient() {
        OAuthToken token = new OAuthToken();
        Client client = new Client();

        Assert.assertNull(token.getClient());
        token.setClient(client);
        Assert.assertEquals(client, token.getClient());
    }

    /**
     * Assert that we can get and set the token type.
     */
    @Test
    public void testGetSetTokenType() {
        OAuthToken c = new OAuthToken();

        // Default
        Assert.assertEquals(OAuthTokenType.Bearer, c.getTokenType());
        c.setTokenType(OAuthTokenType.Authorization);
        Assert.assertEquals(OAuthTokenType.Authorization, c.getTokenType());
    }

    /**
     * Assert that we can get and set the expiration date.
     */
    @Test
    public void testGetSetExpiration() {
        OAuthToken c = new OAuthToken();

        // Default
        Assert.assertEquals(600, c.getExpiresIn());
        c.setExpiresIn(100);
        Assert.assertEquals(100, c.getExpiresIn());
    }

    /**
     * Test setting a related token (such as an access token for a refresh
     * token).
     */
    @Test
    public void testGetSetToken() {
        OAuthToken token = new OAuthToken();
        OAuthToken otherToken = new OAuthToken();

        Assert.assertNull(token.getAuthToken());
        token.setAuthToken(otherToken);
        Assert.assertEquals(otherToken, token.getAuthToken());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetScopes() {
        OAuthToken token = new OAuthToken();
        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        scopes.put("test", new ApplicationScope());

        Assert.assertNull(token.getScopes());
        token.setScopes(scopes);
        Assert.assertEquals(scopes, token.getScopes());
        Assert.assertNotSame(scopes, token.getScopes());
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
        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());

        Client client = new Client();
        client.setId(UUID.randomUUID());

        OAuthToken token = new OAuthToken();
        token.setId(UUID.randomUUID());
        token.setCreatedDate(Calendar.getInstance());
        token.setModifiedDate(Calendar.getInstance());
        token.setIdentity(identity);
        token.setClient(client);
        token.setTokenType(OAuthTokenType.Authorization);
        token.setExpiresIn(100);

        // De/serialize to json.
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(token);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                token.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(token.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(token.getCreatedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                token.getTokenType().toString(),
                node.get("tokenType").asText());
        Assert.assertEquals(
                token.getExpiresIn(),
                node.get("expiresIn").asLong());


        Assert.assertFalse(node.has("client"));
        Assert.assertFalse(node.has("identity"));

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
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        ObjectNode node = m.createObjectNode();
        node.put("id", UUID.randomUUID().toString());
        node.put("createdDate",
                format.format(Calendar.getInstance().getTime()));
        node.put("modifiedDate",
                format.format(Calendar.getInstance().getTime()));
        node.put("accessToken", "accessToken");
        node.put("tokenType", "Authorization");
        node.put("expiresIn", 300);

        String output = m.writeValueAsString(node);
        OAuthToken c = m.readValue(output, OAuthToken.class);

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
                c.getTokenType().toString(),
                node.get("tokenType").asText());
        Assert.assertEquals(
                c.getExpiresIn(),
                node.get("expiresIn").asLong());
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
        OAuthToken c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, c.getId());
    }

}
