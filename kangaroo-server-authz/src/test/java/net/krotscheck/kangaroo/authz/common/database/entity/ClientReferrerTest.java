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
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Test the client referrer entity.
 *
 * @author Michael Krotscheck
 */
public final class ClientReferrerTest {

    /**
     * Assert that we can get and set the referrers's client.
     */
    @Test
    public void testGetSetClient() {
        ClientReferrer referrer = new ClientReferrer();
        Client client = new Client();

        assertNull(referrer.getClient());
        referrer.setClient(client);
        assertEquals(client, referrer.getClient());
    }

    /**
     * Assert that we can get and set the referrers's uri.
     */
    @Test
    public void testGetSetUri() {
        ClientReferrer referrer = new ClientReferrer();
        URI uri = URI.create("http://example.com/");

        assertNull(referrer.getUri());
        referrer.setUri(uri);
        assertEquals(uri, referrer.getUri());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    public void testGetOwner() {
        ClientReferrer referrer = new ClientReferrer();
        Client spy = spy(new Client());

        // Null check
        assertNull(referrer.getOwner());

        referrer.setClient(spy);
        referrer.getOwner();

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

        ClientReferrer r = new ClientReferrer();
        r.setClient(client);
        r.setUri(URI.create("http://example.com/"));
        r.setId(IdUtil.next());
        r.setCreatedDate(Calendar.getInstance());
        r.setModifiedDate(Calendar.getInstance());

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(r);
        JsonNode node = m.readTree(output);

        assertEquals(
                IdUtil.toString(r.getId()),
                node.get("id").asText());
        assertEquals(
                r.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        assertEquals(
                r.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        assertEquals(
                r.getUri().toString(),
                node.get("uri").asText());

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        assertEquals(4, names.size());
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
        ClientReferrer r = m.readValue(output, ClientReferrer.class);

        assertEquals(
                IdUtil.toString(r.getId()),
                node.get("id").asText());
        assertEquals(
                r.getCreatedDate().getTimeInMillis() / 1000,
                node.get("createdDate").asLong());
        assertEquals(
                r.getModifiedDate().getTimeInMillis() / 1000,
                node.get("modifiedDate").asLong());

        assertEquals(
                r.getUri().toString(),
                node.get("uri").asText());

        // Client is not publicly serialized.
        assertNull(r.getClient());
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

        assertEquals(newId, c.getId());
    }
}
