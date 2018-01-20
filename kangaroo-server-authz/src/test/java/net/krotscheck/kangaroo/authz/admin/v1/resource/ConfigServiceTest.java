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

package net.krotscheck.kangaroo.authz.admin.v1.resource;

import net.krotscheck.kangaroo.authz.admin.v1.resource.ConfigService.ConfigurationEntity;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.Config;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Session;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the configuration service.
 */
public final class ConfigServiceTest extends AbstractResourceTest {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<AbstractEntity>>
            LIST_TYPE =
            new GenericType<ListResponseEntity<AbstractEntity>>() {

            };

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<AbstractEntity>> getListType() {
        return LIST_TYPE;
    }

    /**
     * This service is world accessible.
     *
     * @return null.
     */
    @Override
    protected String getAdminScope() {
        return null;
    }

    /**
     * This service is world accessible.
     *
     * @return null.
     */
    @Override
    protected String getRegularScope() {
        return null;
    }

    /**
     * There is no id on this service.
     *
     * @return null.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return null;
    }

    /**
     * There are no entities in this service.
     *
     * @return null.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        return null;
    }

    /**
     * A test.
     */
    @Test
    public void testBasicUsage() {
        Session session = getSession();
        Configuration config = getSystemConfig();
        BigInteger applicationId = IdUtil.fromString(config
                .getString(Config.APPLICATION_ID));
        BigInteger clientId = IdUtil.fromString(config
                .getString(Config.APPLICATION_CLIENT_ID));

        Application a = session.get(Application.class, applicationId);

        Response r = target("/config")
                .request()
                .get();

        assertEquals(200, r.getStatus());

        ConfigurationEntity entity = r.readEntity(ConfigurationEntity.class);

        a.getScopes().keySet()
                .forEach(s -> assertTrue(entity.getScopes().contains(s)));
        assertEquals(entity.getClient(), clientId);
    }
}
