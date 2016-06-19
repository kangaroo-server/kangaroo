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
import net.krotscheck.features.database.entity.UserIdentity.Deserializer;
import net.krotscheck.test.JacksonUtil;
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
 * Unit test for the User Identity entity.
 *
 * @author Michael Krotscheck
 */
public final class UserIdentityTest {

    /**
     * Test getting/setting the user.
     */
    @Test
    public void testGetSetUser() {
        UserIdentity identity = new UserIdentity();
        User user = new User();

        Assert.assertNull(identity.getUser());
        identity.setUser(user);
        Assert.assertEquals(user, identity.getUser());
    }

    /**
     * Test getting/setting the authenticator.
     */
    @Test
    public void testGetSetAuthenticator() {
        UserIdentity identity = new UserIdentity();
        Authenticator authenticator = new Authenticator();

        Assert.assertNull(identity.getAuthenticator());
        identity.setAuthenticator(authenticator);
        Assert.assertEquals(authenticator, identity.getAuthenticator());
    }

    /**
     * Test get/set tokens list.
     */
    @Test
    public void testGetSetTokens() {
        UserIdentity identity = new UserIdentity();
        List<OAuthToken> tokens = new ArrayList<>();
        tokens.add(new OAuthToken());

        Assert.assertNull(identity.getTokens());
        identity.setTokens(tokens);
        Assert.assertEquals(tokens, identity.getTokens());
        Assert.assertNotSame(tokens, identity.getTokens());
    }

    /**
     * Test the remote ID.
     */
    @Test
    public void testGetSetRemoteId() {
        UserIdentity identity = new UserIdentity();

        Assert.assertNull(identity.getRemoteId());
        identity.setRemoteId("foo");
        Assert.assertEquals("foo", identity.getRemoteId());
    }

    /**
     * Test the name.
     */
    @Test
    public void testGetName() {
        UserIdentity identity = new UserIdentity();

        Assert.assertNull(identity.getName());
        identity.setRemoteId("foo");
        Assert.assertEquals("foo", identity.getName());
    }

    /**
     * Test getting/setting the claims.
     */
    @Test
    public void testGetSetClaims() {
        UserIdentity identity = new UserIdentity();
        Map<String, String> claims = new HashMap<>();

        Assert.assertNull(identity.getClaims());
        identity.setClaims(claims);
        Assert.assertEquals(claims, identity.getClaims());
        Assert.assertNotSame(claims, identity.getClaims());
    }

    /**
     * Test the salt.
     */
    @Test
    public void testGetSetSalt() {
        UserIdentity identity = new UserIdentity();
        String testString = "zomg";

        Assert.assertNull(identity.getSalt());
        identity.setSalt(testString);
        Assert.assertEquals(testString, identity.getSalt());
    }

    /**
     * Test the Password.
     */
    @Test
    public void testGetSetPassword() {
        UserIdentity identity = new UserIdentity();
        String testString = "zomg";

        Assert.assertNull(identity.getPassword());
        identity.setPassword(testString);
        Assert.assertEquals(testString, identity.getPassword());
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and
     * doesn't carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());

        Authenticator authenticator = new Authenticator();
        authenticator.setId(UUID.randomUUID());

        List<OAuthToken> tokens = new ArrayList<>();
        OAuthToken token = new OAuthToken();
        token.setId(UUID.randomUUID());
        tokens.add(token);

        Map<String, String> claims = new HashMap<>();
        claims.put("one", "value");
        claims.put("two", "value");

        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identity.setCreatedDate(Calendar.getInstance());
        identity.setModifiedDate(Calendar.getInstance());
        identity.setAuthenticator(authenticator);
        identity.setUser(user);
        identity.setTokens(tokens);
        identity.setClaims(claims);
        identity.setPassword("newpass");
        identity.setSalt("newsalt");
        identity.setRemoteId("remoteId");

        // De/serialize to json.
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(identity);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                identity.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(identity.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(identity.getCreatedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                identity.getAuthenticator().getId().toString(),
                node.get("authenticator").asText());
        Assert.assertEquals(
                identity.getUser().getId().toString(),
                node.get("user").asText());
        Assert.assertEquals(
                identity.getRemoteId(),
                node.get("remoteId").asText());

        // Get the claims node.
        JsonNode claimsNode = node.get("claims");
        Assert.assertEquals(
                "value",
                claimsNode.get("one").asText());
        Assert.assertEquals(
                "value",
                claimsNode.get("two").asText());

        Assert.assertFalse(node.has("tokens"));
        Assert.assertFalse(node.has("password"));
        Assert.assertFalse(node.has("salt"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(7, names.size());
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
        node.put("remoteId", "remoteId");

        ObjectNode claimNode = m.createObjectNode();
        claimNode.put("one", "value");
        claimNode.put("two", "value");
        node.set("claims", claimNode);

        String output = m.writeValueAsString(node);
        UserIdentity a = m.readValue(output, UserIdentity.class);

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
                a.getRemoteId(),
                node.get("remoteId").asText());

        Map<String, String> claims = a.getClaims();

        Assert.assertEquals(
                claims.get("one"),
                claimNode.get("one").asText());
        Assert.assertEquals(
                claims.get("two"),
                claimNode.get("two").asText());
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
        UserIdentity u = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, u.getId());
    }
}
