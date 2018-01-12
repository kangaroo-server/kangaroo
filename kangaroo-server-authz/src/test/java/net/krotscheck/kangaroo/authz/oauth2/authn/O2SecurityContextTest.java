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

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the security context.
 *
 * @author Michael Krotscheck
 */
public final class O2SecurityContextTest {

    /**
     * Assert the principal getter works.
     */
    @Test
    public void getUserPrincipal() {
        O2Principal p = new O2Principal();
        O2SecurityContext context = new O2SecurityContext(p);

        assertSame(p, context.getUserPrincipal());
    }

    /**
     * Assert that this always returns false.
     */
    @Test
    public void isUserInRole() {
        O2Principal p = new O2Principal();
        O2SecurityContext context = new O2SecurityContext(p);

        assertFalse(context.isUserInRole("some_random_role"));
    }

    /**
     * Assert that this always returns true.
     */
    @Test
    public void isSecure() {
        O2Principal p = new O2Principal();
        O2SecurityContext context = new O2SecurityContext(p);

        assertTrue(context.isSecure());
    }

    /**
     * Assert that this returns the correct value based on the authorizing
     * client.
     */
    @Test
    public void getAuthenticationScheme() {
        Client c1 = new Client();
        c1.setId(IdUtil.next());
        c1.setClientSecret("secret");
        O2Principal p1 = new O2Principal(c1);
        O2SecurityContext context1 = new O2SecurityContext(p1);

        assertEquals(O2AuthScheme.ClientPrivate.toString(),
                context1.getAuthenticationScheme());

        Client c2 = new Client();
        c2.setId(IdUtil.next());
        O2Principal p2 = new O2Principal(c2);
        O2SecurityContext context2 = new O2SecurityContext(p2);
        assertEquals(O2AuthScheme.ClientPublic.toString(),
                context2.getAuthenticationScheme());

        O2Principal p3 = new O2Principal();
        O2SecurityContext context3 = new O2SecurityContext(p3);
        assertEquals(O2AuthScheme.None.toString(),
                context3.getAuthenticationScheme());
    }
}
