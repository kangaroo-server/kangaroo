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
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken.Deserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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
     * Test setting the issuer.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testGetSetIssuer() throws Exception {
        OAuthToken token = new OAuthToken();

        Assert.assertNull(token.getIssuer());
        token.setIssuer("test");
        Assert.assertEquals("test", token.getIssuer());
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
    public void testGetOwner() {
        OAuthToken token = new OAuthToken();
        Client spy = spy(new Client());

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
        identity.setId(IdUtil.next());

        Client client = new Client();
        client.setType(ClientType.ClientCredentials);
        client.setId(IdUtil.next());

        OAuthToken token = new OAuthToken();
        token.setId(IdUtil.next());
        token.setCreatedDate(Calendar.getInstance());
        token.setModifiedDate(Calendar.getInstance());
        token.setIdentity(identity);
        token.setClient(client);
        token.setRedirect(new URI("http://example.com/"));
        token.setTokenType(OAuthTokenType.Authorization);
        token.setExpiresIn(100);
        token.setIssuer("localhost");

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(token);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                IdUtil.toString(token.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                token.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                token.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

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
                IdUtil.toString(token.getClient().getId()),
                node.get("client").asText());
        Assert.assertEquals(
                IdUtil.toString(token.getIdentity().getId()),
                node.get("identity").asText());
        Assert.assertEquals(token.getIssuer(),
                node.get("issuer").asText());

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
        ObjectMapper m = new ObjectMapperFactory().get();
        long timestamp = Calendar.getInstance().getTimeInMillis() / 1000;
        ObjectNode node = m.createObjectNode();
        node.put("id", IdUtil.toString(IdUtil.next()));
        node.put("createdDate", timestamp);
        node.put("modifiedDate", timestamp);
        node.put("accessToken", "accessToken");
        node.put("tokenType", "Authorization");
        node.put("expiresIn", 300);
        node.put("redirect", "http://example.com");
        node.put("issuer", "localhost");
        node.put("identity", IdUtil.toString(IdUtil.next()));
        node.put("client", IdUtil.toString(IdUtil.next()));
        node.put("authToken", IdUtil.toString(IdUtil.next()));

        String output = m.writeValueAsString(node);
        OAuthToken c = m.readValue(output, OAuthToken.class);

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
                c.getTokenType().toString(),
                node.get("tokenType").asText());
        Assert.assertEquals(
                c.getExpiresIn().longValue(),
                node.get("expiresIn").asLong());
        Assert.assertEquals(
                c.getIssuer(),
                node.get("issuer").asText());
        Assert.assertEquals(
                c.getRedirect().toString(),
                node.get("redirect").asText());
        Assert.assertEquals(
                IdUtil.toString(c.getIdentity().getId()),
                node.get("identity").asText());
        Assert.assertEquals(
                IdUtil.toString(c.getClient().getId()),
                node.get("client").asText());
        Assert.assertEquals(
                IdUtil.toString(c.getAuthToken().getId()),
                node.get("authToken").asText());
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
        OAuthToken c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(newId, c.getId());
    }
}
