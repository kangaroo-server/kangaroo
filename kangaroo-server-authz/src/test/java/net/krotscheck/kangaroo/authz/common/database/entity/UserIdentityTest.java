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
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity.Deserializer;
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

        assertNull(identity.getUser());
        identity.setUser(user);
        assertEquals(user, identity.getUser());
    }

    /**
     * Test getting/setting the authenticator.
     */
    @Test
    public void testGetSetType() {
        UserIdentity identity = new UserIdentity();

        assertNull(identity.getType());
        identity.setType(AuthenticatorType.Test);
        assertEquals(AuthenticatorType.Test, identity.getType());
    }

    /**
     * Test get/set tokens list.
     */
    @Test
    public void testGetSetTokens() {
        UserIdentity identity = new UserIdentity();
        List<OAuthToken> tokens = new ArrayList<>();
        tokens.add(new OAuthToken());

        assertEquals(0, identity.getTokens().size());
        identity.setTokens(tokens);
        assertEquals(tokens, identity.getTokens());
        assertNotSame(tokens, identity.getTokens());
    }

    /**
     * Test the remote ID.
     */
    @Test
    public void testGetSetRemoteId() {
        UserIdentity identity = new UserIdentity();

        assertNull(identity.getRemoteId());
        identity.setRemoteId("foo");
        assertEquals("foo", identity.getRemoteId());
    }

    /**
     * Test getting/setting the claims.
     */
    @Test
    public void testGetSetClaims() {
        UserIdentity identity = new UserIdentity();
        Map<String, String> claims = new HashMap<>();

        assertEquals(0, identity.getClaims().size());
        identity.setClaims(claims);
        assertEquals(claims, identity.getClaims());
        assertNotSame(claims, identity.getClaims());
    }

    /**
     * Test the salt.
     */
    @Test
    public void testGetSetSalt() {
        UserIdentity identity = new UserIdentity();
        String testString = "zomg";

        assertNull(identity.getSalt());
        identity.setSalt(testString);
        assertEquals(testString, identity.getSalt());
    }

    /**
     * Test the Password.
     */
    @Test
    public void testGetSetPassword() {
        UserIdentity identity = new UserIdentity();
        String testString = "zomg";

        assertNull(identity.getPassword());
        identity.setPassword(testString);
        assertEquals(testString, identity.getPassword());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    public void testGetOwner() {
        UserIdentity identity = new UserIdentity();
        User spy = spy(new User());

        // Null check
        assertNull(identity.getOwner());

        identity.setUser(spy);
        identity.getOwner();

        Mockito.verify(spy).getOwner();
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
        user.setId(IdUtil.next());

        Authenticator authenticator = new Authenticator();
        authenticator.setType(AuthenticatorType.Test);
        authenticator.setId(IdUtil.next());

        List<OAuthToken> tokens = new ArrayList<>();
        OAuthToken token = new OAuthToken();
        token.setId(IdUtil.next());
        tokens.add(token);

        Map<String, String> claims = new HashMap<>();
        claims.put("one", "value");
        claims.put("two", "value");

        UserIdentity identity = new UserIdentity();
        identity.setId(IdUtil.next());
        identity.setCreatedDate(Calendar.getInstance());
        identity.setModifiedDate(Calendar.getInstance());
        identity.setType(authenticator.getType());
        identity.setUser(user);
        identity.setTokens(tokens);
        identity.setClaims(claims);
        identity.setPassword("newpass");
        identity.setSalt("newsalt");
        identity.setRemoteId("remoteId");

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(identity);
        JsonNode node = m.readTree(output);

        assertEquals(
                IdUtil.toString(identity.getId()),
                node.get("id").asText());
        assertEquals(
                identity.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        assertEquals(
                identity.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        assertEquals(
                identity.getType().toString(),
                node.get("type").asText());
        assertEquals(
                IdUtil.toString(identity.getUser().getId()),
                node.get("user").asText());
        assertEquals(
                identity.getRemoteId(),
                node.get("remoteId").asText());

        // Get the claims node.
        JsonNode claimsNode = node.get("claims");
        assertEquals(
                "value",
                claimsNode.get("one").asText());
        assertEquals(
                "value",
                claimsNode.get("two").asText());

        assertFalse(node.has("tokens"));
        // We are not testing for password/salt, as those are
        // handled by view inclusion
//        assertFalse(node.has("password"));
//        assertFalse(node.has("salt"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        assertEquals(9, names.size());
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
        node.put("remoteId", "remoteId");
        node.put("authenticator", IdUtil.toString(IdUtil.next()));
        node.put("user", IdUtil.toString(IdUtil.next()));

        ObjectNode claimNode = m.createObjectNode();
        claimNode.put("one", "value");
        claimNode.put("two", "value");
        node.set("claims", claimNode);

        String output = m.writeValueAsString(node);
        UserIdentity a = m.readValue(output, UserIdentity.class);

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
                a.getRemoteId(),
                node.get("remoteId").asText());
        assertEquals(
                IdUtil.toString(a.getUser().getId()),
                node.get("user").asText());

        Map<String, String> claims = a.getClaims();

        assertEquals(
                claims.get("one"),
                claimNode.get("one").asText());
        assertEquals(
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
        BigInteger newId = IdUtil.next();
        String id = String.format("\"%s\"", IdUtil.toString(newId));
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        UserIdentity u = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        assertEquals(newId, u.getId());
    }
}
