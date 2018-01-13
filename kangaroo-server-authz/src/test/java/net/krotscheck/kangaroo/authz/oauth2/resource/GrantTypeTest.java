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
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("\"authorization_code\"", auth);

        String cc = m.writeValueAsString(GrantType.ClientCredentials);
        Assert.assertEquals("\"client_credentials\"", cc);

        String pswd = m.writeValueAsString(GrantType.Password);
        Assert.assertEquals("\"password\"", pswd);

        String refresh = m.writeValueAsString(GrantType.RefreshToken);
        Assert.assertEquals("\"refresh_token\"", refresh);
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
        Assert.assertSame(auth, GrantType.AuthorizationCode);

        GrantType implicit =
                m.readValue("\"client_credentials\"", GrantType.class);
        Assert.assertSame(implicit, GrantType.ClientCredentials);

        GrantType owner =
                m.readValue("\"password\"", GrantType.class);
        Assert.assertSame(owner, GrantType.Password);

        GrantType client =
                m.readValue("\"refresh_token\"", GrantType.class);
        Assert.assertSame(client, GrantType.RefreshToken);
    }

    /**
     * Assert that valueOf conversions works.
     */
    @Test
    public void testFromString() {
        Assert.assertEquals(
                GrantType.AuthorizationCode,
                GrantType.fromString("authorization_code")
        );
        Assert.assertEquals(
                GrantType.ClientCredentials,
                GrantType.fromString("client_credentials")
        );
        Assert.assertEquals(
                GrantType.Password,
                GrantType.fromString("password")
        );
        Assert.assertEquals(
                GrantType.RefreshToken,
                GrantType.fromString("refresh_token")
        );
    }

    /**
     * Assert that valueOf conversions works.
     */
    @Test
    public void testValueOf() {
        Assert.assertEquals(
                GrantType.AuthorizationCode,
                GrantType.valueOf("AuthorizationCode")
        );
        Assert.assertEquals(
                GrantType.ClientCredentials,
                GrantType.valueOf("ClientCredentials")
        );
        Assert.assertEquals(
                GrantType.Password,
                GrantType.valueOf("Password")
        );
        Assert.assertEquals(
                GrantType.RefreshToken,
                GrantType.valueOf("RefreshToken")
        );
    }
}
