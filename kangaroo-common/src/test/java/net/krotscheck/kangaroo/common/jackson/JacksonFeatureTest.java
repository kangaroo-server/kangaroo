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

import net.krotscheck.kangaroo.common.hibernate.entity.TestEntity;
import net.krotscheck.kangaroo.test.jersey.KangarooJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;


/**
 * Test the jackson feature injection.
 * ss
 *
 * @author Michael Krotscheck
 */
public final class JacksonFeatureTest extends KangarooJerseyTest {

    /**
     * Build an application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(JacksonFeature.class);
        config.register(MockService.class);
        return config;
    }

    /**
     * Assert that the jackson feature is available.
     */
    @Test
    public void testProperDeserialization() {
        TestEntity entity = new TestEntity();
        Entity pojoEntity = Entity.entity(entity,
                MediaType.APPLICATION_JSON_TYPE);
        TestEntity response = target("/").request()
                .post(pojoEntity, TestEntity.class);

        assertEquals(entity.getId(), response.getId());
    }

    /**
     * A simple endpoint that returns the entity, to test a full circuit.
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
        public Response modifyPojo(final TestEntity pojo) {
            return Response.ok(pojo).build();
        }
    }
}
