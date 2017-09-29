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
import net.krotscheck.kangaroo.authz.common.database.entity.Client.Deserializer;
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
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Test the client referrer entity.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
public final class ClientReferrerTest {

    /**
     * Assert that we can get and set the referrers's client.
     */
    @Test
    public void testGetSetClient() {
        ClientReferrer referrer = new ClientReferrer();
        Client client = new Client();

        Assert.assertNull(referrer.getClient());
        referrer.setClient(client);
        Assert.assertEquals(client, referrer.getClient());
    }

    /**
     * Assert that we can get and set the referrers's uri.
     */
    @Test
    public void testGetSetUri() {
        ClientReferrer referrer = new ClientReferrer();
        URI uri = URI.create("http://example.com/");

        Assert.assertNull(referrer.getUri());
        referrer.setUri(uri);
        Assert.assertEquals(uri, referrer.getUri());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    @PrepareForTest(Client.class)
    public void testGetOwner() {
        ClientReferrer referrer = new ClientReferrer();
        Client spy = PowerMockito.spy(new Client());

        // Null check
        Assert.assertNull(referrer.getOwner());

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
        client.setId(UUID.randomUUID());

        ClientReferrer r = new ClientReferrer();
        r.setClient(client);
        r.setUri(URI.create("http://example.com/"));
        r.setId(UUID.randomUUID());
        r.setCreatedDate(Calendar.getInstance());
        r.setModifiedDate(Calendar.getInstance());

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(r);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                r.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(r.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(r.getCreatedDate().getTime()),
                node.get("modifiedDate").asText());

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
        DateFormat format = new ISO8601DateFormat();
        ObjectNode node = m.createObjectNode();
        node.put("id", UUID.randomUUID().toString());
        node.put("createdDate",
                format.format(Calendar.getInstance().getTime()));
        node.put("modifiedDate",
                format.format(Calendar.getInstance().getTime()));
        node.put("client", UUID.randomUUID().toString());
        node.put("uri", "http://example.com/");

        String output = m.writeValueAsString(node);
        ClientReferrer r = m.readValue(output, ClientReferrer.class);

        Assert.assertEquals(
                r.getId().toString(),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(r.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(r.getModifiedDate().getTime()),
                node.get("modifiedDate").asText());

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
        UUID uuid = UUID.randomUUID();
        String id = String.format("\"%s\"", uuid);
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        Client c = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(uuid, c.getId());
    }
}
