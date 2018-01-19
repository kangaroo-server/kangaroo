/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.oauth2.authn;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


/**
 * Unit test for the Scheme type.
 *
 * @author Michael Krotscheck
 */
public final class O2AuthSchemeTest {

    /**
     * Assert that these enum types serialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testSerialization() throws Exception {
        ObjectMapper m = new ObjectMapperFactory().get();

        String auth = m.writeValueAsString(O2AuthScheme.ClientPrivate);
        assertEquals("\"ClientPrivate\"", auth);

        String implicit = m.writeValueAsString(O2AuthScheme.ClientPublic);
        assertEquals("\"ClientPublic\"", implicit);

        String bearerToken = m.writeValueAsString(O2AuthScheme.BearerToken);
        assertEquals("\"BearerToken\"", bearerToken);

        String owner = m.writeValueAsString(O2AuthScheme.None);
        assertEquals("\"None\"", owner);
    }

    /**
     * Assert that these enum types serialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testDeserialization() throws Exception {
        ObjectMapper m = new ObjectMapperFactory().get();

        O2AuthScheme auth =
                m.readValue("\"ClientPrivate\"", O2AuthScheme.class);
        assertSame(auth, O2AuthScheme.ClientPrivate);

        O2AuthScheme implicit =
                m.readValue("\"ClientPublic\"", O2AuthScheme.class);
        assertSame(implicit, O2AuthScheme.ClientPublic);

        O2AuthScheme bearerToken =
                m.readValue("\"BearerToken\"", O2AuthScheme.class);
        assertSame(bearerToken, O2AuthScheme.BearerToken);

        O2AuthScheme owner =
                m.readValue("\"None\"", O2AuthScheme.class);
        assertSame(owner, O2AuthScheme.None);
    }

    /**
     * Assert that valueOf conversions works.
     */
    @Test
    public void testValueOf() {
        assertEquals(
                O2AuthScheme.ClientPrivate,
                O2AuthScheme.valueOf("ClientPrivate")
        );
        assertEquals(
                O2AuthScheme.ClientPublic,
                O2AuthScheme.valueOf("ClientPublic")
        );
        assertEquals(
                O2AuthScheme.BearerToken,
                O2AuthScheme.valueOf("BearerToken")
        );
        assertEquals(
                O2AuthScheme.None,
                O2AuthScheme.valueOf("None")
        );
    }

    /**
     * Assert that the metadata is correct.
     */
    @Test
    public void testType() {
        assertTrue(O2AuthScheme.ClientPrivate.isAuth());
        assertTrue(O2AuthScheme.BearerToken.isAuth());
        assertFalse(O2AuthScheme.ClientPublic.isAuth());
        assertFalse(O2AuthScheme.None.isAuth());
    }
}
