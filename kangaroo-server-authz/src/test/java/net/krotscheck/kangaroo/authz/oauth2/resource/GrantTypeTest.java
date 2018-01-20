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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unti test for the grant type enumeration and its conversion methods.
 *
 * @author Michael Krotscheck
 */
public final class GrantTypeTest {

    /**
     * Assert that these enum types serialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testSerialization() throws Exception {
        ObjectMapper m = new ObjectMapperFactory().get();

        String auth = m.writeValueAsString(GrantType.AuthorizationCode);
        assertEquals("\"authorization_code\"", auth);

        String cc = m.writeValueAsString(GrantType.ClientCredentials);
        assertEquals("\"client_credentials\"", cc);

        String pswd = m.writeValueAsString(GrantType.Password);
        assertEquals("\"password\"", pswd);

        String refresh = m.writeValueAsString(GrantType.RefreshToken);
        assertEquals("\"refresh_token\"", refresh);
    }

    /**
     * Assert that these enum types deserialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testDeserialization() throws Exception {
        ObjectMapper m = new ObjectMapperFactory().get();

        GrantType auth =
                m.readValue("\"authorization_code\"", GrantType.class);
        assertSame(auth, GrantType.AuthorizationCode);

        GrantType implicit =
                m.readValue("\"client_credentials\"", GrantType.class);
        assertSame(implicit, GrantType.ClientCredentials);

        GrantType owner =
                m.readValue("\"password\"", GrantType.class);
        assertSame(owner, GrantType.Password);

        GrantType client =
                m.readValue("\"refresh_token\"", GrantType.class);
        assertSame(client, GrantType.RefreshToken);
    }

    /**
     * Assert that valueOf conversions works.
     */
    @Test
    public void testFromString() {
        assertEquals(
                GrantType.AuthorizationCode,
                GrantType.fromString("authorization_code")
        );
        assertEquals(
                GrantType.ClientCredentials,
                GrantType.fromString("client_credentials")
        );
        assertEquals(
                GrantType.Password,
                GrantType.fromString("password")
        );
        assertEquals(
                GrantType.RefreshToken,
                GrantType.fromString("refresh_token")
        );
    }

    /**
     * Assert that valueOf conversions works.
     */
    @Test
    public void testValueOf() {
        assertEquals(
                GrantType.AuthorizationCode,
                GrantType.valueOf("AuthorizationCode")
        );
        assertEquals(
                GrantType.ClientCredentials,
                GrantType.valueOf("ClientCredentials")
        );
        assertEquals(
                GrantType.Password,
                GrantType.valueOf("Password")
        );
        assertEquals(
                GrantType.RefreshToken,
                GrantType.valueOf("RefreshToken")
        );
    }
}
