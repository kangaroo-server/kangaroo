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

package net.krotscheck.kangaroo.common.httpClient;

import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the HTTP Client test.
 *
 * @author Michael Krotscheck
 */
public class HttpClientFactoryTest {

    /**
     * Assert that the client builder can be created.
     */
    @Test
    public void assertCanCreateClient() {
        JerseyClientBuilderFactory builder = new JerseyClientBuilderFactory();
        HttpClientFactory factory = new HttpClientFactory(builder.get());
        assertNotNull(factory.get());
    }

    /**
     * Assert that the client builder can be created.
     */
    @Test
    public void assertCanDisposeClient() {
        Client testClient = mock(Client.class);
        JerseyClientBuilderFactory builder = new JerseyClientBuilderFactory();
        HttpClientFactory factory = new HttpClientFactory(builder.get());
        factory.dispose(testClient);
        verify(testClient, times(1)).close();
    }

    /**
     * Assert that the client builder can be created.
     */
    @Test
    public void assertCanUseClient() {
        JerseyClientBuilderFactory builder = new JerseyClientBuilderFactory();
        HttpClientFactory factory = new HttpClientFactory(builder.get());
        Client client = factory.get();

        Response r = client.target("https://www.example.com/")
                .request()
                .get();

        assertEquals(200, r.getStatus());

        factory.dispose(client);
    }
}
