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

import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.authz.common.database.util.JacksonUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the token type.
 *
 * @author Michael Krotscheck
 */
public final class OAuthTokenTypeTest {

    /**
     * Assert that these enum types serialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testSerialization() throws Exception {
        ObjectMapper m = JacksonUtil.buildMapper();

        String authOutput = m.writeValueAsString(OAuthTokenType.Authorization);
        Assert.assertEquals("\"Authorization\"", authOutput);

        String bearerOutput = m.writeValueAsString(OAuthTokenType.Bearer);
        Assert.assertEquals("\"Bearer\"", bearerOutput);

        String refreshOutput = m.writeValueAsString(OAuthTokenType.Refresh);
        Assert.assertEquals("\"Refresh\"", refreshOutput);
    }

    /**
     * Assert that these enum types serialize into expected values.
     *
     * @throws Exception Json Serialization Exception.
     */
    @Test
    public void testDeserialization() throws Exception {
        ObjectMapper m = JacksonUtil.buildMapper();
        OAuthTokenType authOutput =
                m.readValue("\"Authorization\"", OAuthTokenType.class);
        Assert.assertSame(authOutput, OAuthTokenType.Authorization);
        OAuthTokenType bearerOutput =
                m.readValue("\"Bearer\"", OAuthTokenType.class);
        Assert.assertSame(bearerOutput, OAuthTokenType.Bearer);
        OAuthTokenType refreshOutput =
                m.readValue("\"Refresh\"", OAuthTokenType.class);
        Assert.assertSame(refreshOutput, OAuthTokenType.Refresh);
    }

    /**
     * Assert that valueOf conversions works.
     */
    @Test
    public void testValueOf() {
        Assert.assertEquals(
                OAuthTokenType.Bearer,
                OAuthTokenType.valueOf("Bearer")
        );
        Assert.assertEquals(
                OAuthTokenType.Authorization,
                OAuthTokenType.valueOf("Authorization")
        );
        Assert.assertEquals(
                OAuthTokenType.Refresh,
                OAuthTokenType.valueOf("Refresh")
        );
    }
}
