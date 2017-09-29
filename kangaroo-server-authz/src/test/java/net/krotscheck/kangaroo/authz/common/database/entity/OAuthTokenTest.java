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
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken.Deserializer;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Unit test for the OAuth Token Entity.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
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
     * Assert that we can get and set the token's http session.
     */
    @Test
    public void testGetSetHttpSession() {
        OAuthToken token = new OAuthToken();
        HttpSession session = new HttpSession();

        Assert.assertNull(token.getHttpSession());
        token.setHttpSession(session);
        Assert.assertEquals(session, token.getHttpSession());
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
        Assert.assertNull(c.getTokenType());
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
        Assert.assertNull(c.getExpiresIn());
        c.setExpiresIn(100);
        Assert.assertEquals(100, c.getExpiresIn().longValue());
        c.setExpiresIn((long) 200);
        Assert.assertEquals(200, c.getExpiresIn().longValue());
        c.setExpiresIn(Double.valueOf(2.222));
        Assert.assertEquals(2, c.getExpiresIn().longValue());
        c.setExpiresIn(Integer.valueOf(22));
        Assert.assertEquals(22, c.getExpiresIn().longValue());
        c.setExpiresIn(Long.valueOf(100));
        Assert.assertEquals(100, c.getExpiresIn().longValue());
        c.setExpiresIn((Long) null);
        Assert.assertNull(c.getExpiresIn());
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
     * Test setting the redirection URL.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetSetRedirect() throws Exception {
        OAuthToken token = new OAuthToken();

        URI test = new URI("http://example.com/");

        Assert.assertNull(token.getRedirect());
        token.setRedirect(test);
        Assert.assertEquals(test, token.getRedirect());
    }

    /**
     * Test getting the principal name for a regular user.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetNameUserIdentity() throws Exception {
        OAuthToken token = new OAuthToken();
        UserIdentity u = new UserIdentity();
        u.setRemoteId("RemoteId");

        Client c = new Client();
        c.setType(ClientType.Implicit);
        c.setName("foo");

        token.setClient(c);
        token.setIdentity(u);

        Assert.assertEquals(u.getRemoteId(), token.getName());
    }

    /**
     * Test getting the principal name for a credentials client.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetNameClient() throws Exception {
        OAuthToken token = new OAuthToken();
        Client c = new Client();
        c.setType(ClientType.ClientCredentials);
        c.setName("foo");

        token.setClient(c);
        Assert.assertEquals(c.getName(), token.getName());
    }

    /**
     * Test getting the principal name with a null client.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetNullClient() throws Exception {
        OAuthToken token = new OAuthToken();
        Assert.assertNull(token.getName());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetScopes() {
        OAuthToken token = new OAuthToken();
        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        scopes.put("test", new ApplicationScope());

        Assert.assertEquals(0, token.getScopes().size());
        token.setScopes(scopes);
        Assert.assertEquals(scopes, token.getScopes());
        Assert.assertNotSame(scopes, token.getScopes());
    }

    /**
     * Test isExpired().
     */
    @Test
    public void testIsExpired() {
        OAuthToken token = new OAuthToken();
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone pdt = TimeZone.getTimeZone("PDT");

        Calendar recentUTC = Calendar.getInstance(utc);
        recentUTC.add(Calendar.SECOND, -100);

        Calendar recentPDT = Calendar.getInstance(pdt);
        recentPDT.add(Calendar.SECOND, -100);

        // Test null createdDate.
        Assert.assertTrue(token.isExpired());

        // Test UTC Non-Expired Token.
        token.setCreatedDate(recentUTC);
        token.setExpiresIn((long) 103);
        Assert.assertFalse(token.isExpired());

        // Expire the token.
        token.setExpiresIn((long) 99);
        Assert.assertTrue(token.isExpired());

        // Test Non-UTC Non-Expired Token.
        token.setCreatedDate(recentPDT);
        token.setExpiresIn((long) 103);
        Assert.assertFalse(token.isExpired());

        // Expire the token.
        token.setExpiresIn((long) 99);
        Assert.assertTrue(token.isExpired());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    @PrepareForTest(Client.class)
    public void testGetOwner() {
        OAuthToken token = new OAuthToken();
        Client spy = PowerMockito.spy(new Client());

        // Null check
        Assert.assertNull(token.getOwner());

        token.setClient(spy);
        token.getOwner();

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
        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());

        Client client = new Client();
        client.setType(ClientType.ClientCredentials);
        client.setId(UUID.randomUUID());

        OAuthToken token = new OAuthToken();
        token.setId(UUID.randomUUID());
        token.setCreatedDate(Calendar.getInstance());
        token.setModifiedDate(Calendar.getInstance());
        token.setIdentity(identity);
        token.setClient(client);
        token.setRedirect(new URI("http://example.com/"));
        token.setTokenType(OAuthTokenType.Authorization);
        token.setExpiresIn(100);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
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
                token.getExpiresIn().longValue(),
                node.get("expiresIn").asLong());
        Assert.assertEquals(
                token.getRedirect().toString(),
                node.get("redirect").asText());
        Assert.assertEquals(
                token.getClient().getId().toString(),
                node.get("client").asText());
        Assert.assertEquals(
                token.getIdentity().getId().toString(),
                node.get("identity").asText());

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
        node.put("redirect", "http://example.com");

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
                c.getExpiresIn().longValue(),
                node.get("expiresIn").asLong());
        Assert.assertEquals(
                c.getRedirect().toString(),
                node.get("redirect").asText());
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
