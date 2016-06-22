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

package net.krotscheck.kangaroo.common.jackson;

import net.krotscheck.kangaroo.common.jackson.mock.MockPojo;
import net.krotscheck.kangaroo.common.jackson.mock.MockPojoDeserializer;
import net.krotscheck.kangaroo.common.jackson.mock.MockPojoSerializer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * Test the jackson feature injection.
 *
 * @author Michael Krotscheck
 */
public final class JacksonFeatureTest extends JerseyTest {

    /**
     * Build an application.
     *
     * @return A configured application.
     */
    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig();
        config.register(JacksonFeature.class);
        config.register(new MockPojoDeserializer.Binder());
        config.register(new MockPojoSerializer.Binder());
        config.register(MockService.class);
        return config;
    }

    /**
     * Assert that the jackson feature is available.
     */
    @Test
    public void testProperDeserialization() {
        MockPojo pojo = new MockPojo();
        Entity pojoEntity = Entity.entity(pojo,
                MediaType.APPLICATION_JSON_TYPE);
        MockPojo response = target("/").request()
                .post(pojoEntity, MockPojo.class);

        Assert.assertTrue(response.isInvokedDeserializer());
        Assert.assertTrue(response.isInvokedSerializer());
        Assert.assertTrue(response.isServiceData());
    }


    /**
     * A simple endpoint that manipulates the MockPojo.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * Update the pojo as it comes through the pipeline.
         *
         * @param pojo The Pojo
         * @return Nothing, error thrown.
         */
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public Response modifyPojo(final MockPojo pojo) {
            pojo.setServiceData(true);
            return Response.ok(pojo).build();
        }

    }
}
