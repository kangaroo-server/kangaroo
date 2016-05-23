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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the client type.
 *
 * @author Michael Krotscheck
 */
public final class ClientTypeTest {

    /**
     * Assert that these enum types serialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testSerialization() throws Exception {
        ObjectMapper m = new ObjectMapper();

        String auth = m.writeValueAsString(ClientType.AuthorizationGrant);
        Assert.assertEquals("\"AuthorizationGrant\"", auth);

        String implicit = m.writeValueAsString(ClientType.Implicit);
        Assert.assertEquals("\"Implicit\"", implicit);

        String owner = m.writeValueAsString(ClientType.OwnerCredentials);
        Assert.assertEquals("\"OwnerCredentials\"", owner);

        String client = m.writeValueAsString(ClientType.ClientCredentials);
        Assert.assertEquals("\"ClientCredentials\"", client);
    }

    /**
     * Assert that these enum types serialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testDeserialization() throws Exception {
        ObjectMapper m = new ObjectMapper();

        ClientType auth =
                m.readValue("\"AuthorizationGrant\"", ClientType.class);
        Assert.assertSame(auth, ClientType.AuthorizationGrant);

        ClientType implicit =
                m.readValue("\"Implicit\"", ClientType.class);
        Assert.assertSame(implicit, ClientType.Implicit);

        ClientType owner =
                m.readValue("\"OwnerCredentials\"", ClientType.class);
        Assert.assertSame(owner, ClientType.OwnerCredentials);

        ClientType client =
                m.readValue("\"ClientCredentials\"", ClientType.class);
        Assert.assertSame(client, ClientType.ClientCredentials);
    }
}
