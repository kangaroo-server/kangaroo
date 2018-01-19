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
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope.Deserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Test the application scope entity.
 *
 * @author Michael Krotscheck
 */
public final class ApplicationScopeTest {

    /**
     * Test getting/setting the owner.
     */
    @Test
    public void testGetSetApplication() {
        ApplicationScope scope = new ApplicationScope();
        Application application = new Application();

        Assert.assertNull(scope.getApplication());
        scope.setApplication(application);
        Assert.assertEquals(application, scope.getApplication());
    }

    /**
     * Test get/set name.
     */
    @Test
    public void testGetSetName() {
        ApplicationScope a = new ApplicationScope();

        Assert.assertNull(a.getName());
        a.setName("foo");
        Assert.assertEquals("foo", a.getName());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetRoles() {
        ApplicationScope scope = new ApplicationScope();
        List<Role> roles = new ArrayList<>();
        roles.add(new Role());

        Assert.assertTrue(scope.getRoles().size() == 0);
        scope.setRoles(roles);
        Assert.assertEquals(roles, scope.getRoles());
        Assert.assertNotSame(roles, scope.getRoles());
    }

    /**
     * Test get/set scope list.
     */
    @Test
    public void testGetSetTokens() {
        ApplicationScope scope = new ApplicationScope();
        List<OAuthToken> tokens = new ArrayList<>();
        tokens.add(new OAuthToken());

        Assert.assertTrue(scope.getTokens().size() == 0);
        scope.setTokens(tokens);
        Assert.assertEquals(tokens, scope.getTokens());
        Assert.assertNotSame(tokens, scope.getTokens());
    }

    /**
     * Assert that we retrieve the owner from the scope's application.
     */
    @Test
    public void testGetOwner() {
        ApplicationScope scope = new ApplicationScope();
        Application spy = spy(new Application());

        // Null check
        Assert.assertNull(scope.getOwner());

        scope.setApplication(spy);
        scope.getOwner();

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

        ApplicationScope a = new ApplicationScope();
        a.setId(IdUtil.next());
        a.setCreatedDate(Calendar.getInstance());
        a.setModifiedDate(Calendar.getInstance());
        a.setApplication(application);
        a.setName("name");

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(a);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                IdUtil.toString(a.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                a.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                a.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());
        Assert.assertEquals(
                IdUtil.toString(a.getApplication().getId()),
                node.get("application").asText());
        Assert.assertEquals(
                a.getName(),
                node.get("name").asText());

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
        ObjectMapper m = new ObjectMapperFactory().get();
        long timestamp = Calendar.getInstance().getTimeInMillis() / 1000;
        ObjectNode node = m.createObjectNode();
        node.put("id", IdUtil.toString(IdUtil.next()));
        node.put("createdDate", timestamp);
        node.put("modifiedDate", timestamp);
        node.put("name", "name");
        node.put("application", IdUtil.toString(IdUtil.next()));

        String output = m.writeValueAsString(node);
        ApplicationScope a = m.readValue(output, ApplicationScope.class);

        Assert.assertEquals(
                IdUtil.toString(a.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                a.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                a.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());
        Assert.assertEquals(
                a.getName(),
                node.get("name").asText());
        Assert.assertEquals(
                IdUtil.toString(a.getApplication().getId()),
                node.get("application").asText());
    }

    /**
     * Test the application deserializer.
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
        ApplicationScope a = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(newId, a.getId());
    }
}
