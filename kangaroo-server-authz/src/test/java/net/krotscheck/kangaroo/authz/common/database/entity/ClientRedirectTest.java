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
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Test the client redirect entity.
 *
 * @author Michael Krotscheck
 */
public final class ClientRedirectTest {

    /**
     * Assert that we can get and set the redirects's client.
     */
    @Test
    public void testGetSetClient() {
        ClientRedirect redirect = new ClientRedirect();
        Client client = new Client();

        Assert.assertNull(redirect.getClient());
        redirect.setClient(client);
        Assert.assertEquals(client, redirect.getClient());
    }

    /**
     * Assert that we can get and set the redirects's uri.
     */
    @Test
    public void testGetSetUri() {
        ClientRedirect redirect = new ClientRedirect();
        URI uri = URI.create("http://example.com/");

        Assert.assertNull(redirect.getUri());
        redirect.setUri(uri);
        Assert.assertEquals(uri, redirect.getUri());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    public void testGetOwner() {
        ClientRedirect redirect = new ClientRedirect();
        Client spy = spy(new Client());

        // Null check
        Assert.assertNull(redirect.getOwner());

        redirect.setClient(spy);
        redirect.getOwner();

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
        Client client = new Client();
        client.setId(IdUtil.next());

        ClientRedirect r = new ClientRedirect();
        r.setClient(client);
        r.setUri(URI.create("http://example.com/"));
        r.setId(IdUtil.next());
        r.setCreatedDate(Calendar.getInstance());
        r.setModifiedDate(Calendar.getInstance());

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(r);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                IdUtil.toString(r.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                r.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                r.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        Assert.assertEquals(
                r.getUri().toString(),
                node.get("uri").asText());

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(4, names.size());
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
        node.put("client", IdUtil.toString(IdUtil.next()));
        node.put("uri", "http://example.com/");

        String output = m.writeValueAsString(node);
        ClientRedirect r = m.readValue(output, ClientRedirect.class);

        Assert.assertEquals(
                IdUtil.toString(r.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                r.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        Assert.assertEquals(
                r.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        Assert.assertEquals(
                r.getUri().toString(),
                node.get("uri").asText());

        // Client is not publicly serialized.
        Assert.assertNull(r.getClient());
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
