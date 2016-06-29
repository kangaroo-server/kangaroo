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

package net.krotscheck.kangaroo.database.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import net.krotscheck.kangaroo.database.entity.User.Deserializer;
import net.krotscheck.kangaroo.database.util.JacksonUtil;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Test the user entity.
 *
 * @author Michael Krotscheck
 */
public final class UserTest {

    /**
     * Assert that we can get and set the application to which this user
     * belongs.
     */
    @Test
    public void testGetSetApplication() {
        User u = new User();
        Application a = new Application();

        Assert.assertNull(u.getApplication());
        u.setApplication(a);
        Assert.assertEquals(a, u.getApplication());
    }

    /**
     * Assert that we can get and set the user's role.
     */
    @Test
    public void testGetSetRole() {
        User user = new User();
        Role role = new Role();

        Assert.assertNull(user.getRole());
        user.setRole(role);
        Assert.assertEquals(role, user.getRole());
    }

    /**
     * Assert that we can get and set the identities.
     */
    @Test
    public void testGetSetIdentities() {
        List<UserIdentity> identities = new ArrayList<>();
        identities.add(new UserIdentity());
        User user = new User();

        Assert.assertNull(user.getIdentities());
        user.setIdentities(identities);
        Assert.assertEquals(identities, user.getIdentities());
        Assert.assertNotSame(identities, user.getIdentities());
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
        List<UserIdentity> identities = new ArrayList<>();
        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identities.add(identity);

        Role role = new Role();
        role.setId(UUID.randomUUID());

        Application application = new Application();
        application.setId(UUID.randomUUID());

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setCreatedDate(Calendar.getInstance());
        user.setModifiedDate(Calendar.getInstance());
        user.setApplication(application);
        user.setRole(role);
        user.setIdentities(identities);

        // De/serialize to json.
        ObjectMapper m = JacksonUtil.buildMapper();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(user);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                user.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(user.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(user.getModifiedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                user.getRole().getId().toString(),
                node.get("role").asText());
        Assert.assertEquals(
                user.getApplication().getId().toString(),
                node.get("application").asText());

        // Should not be serialized.
        Assert.assertFalse(node.has("identities"));

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

        String output = m.writeValueAsString(node);
        User user = m.readValue(output, User.class);

        Assert.assertEquals(
                user.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(user.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(user.getModifiedDate().getTime()),
                node.get("modifiedDate").asText());
    }


    /**
     * Test the user deserializer.
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
        User u = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, u.getId());
    }
}
