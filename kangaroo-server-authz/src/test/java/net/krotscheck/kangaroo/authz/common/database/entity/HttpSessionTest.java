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

import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.hibernate.Session;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * Assert that the HTTP session data is sanely persisted.
 *
 * @author Michael Krotscheck
 */
public final class HttpSessionTest extends DatabaseTest {

    /**
     * Assert that simple database persistence is possible.
     */
    @Test
    public void testSimplePersistence() {
        HttpSession httpSession = new HttpSession();

        assertEquals(-1, httpSession.getSessionTimeout());
        httpSession.setSessionTimeout(1000);

        assertNull(httpSession.getId());

        Session s = getSession();
        s.beginTransaction();
        s.save(httpSession);
        s.getTransaction().commit();

        assertNotNull(httpSession.getId());
        assertEquals(httpSession.getModifiedDate(),
                httpSession.getCreatedDate());
        assertEquals(1000, httpSession.getSessionTimeout());
    }

    /**
     * Assert get refresh tokens.
     */
    @Test
    public void assertGetSetRefreshTokens() {
        HttpSession httpSession = new HttpSession();
        List<OAuthToken> refreshTokens = new ArrayList<>();
        refreshTokens.add(new OAuthToken());

        assertEquals(0, httpSession.getRefreshTokens().size());
        httpSession.setRefreshTokens(refreshTokens);
        assertEquals(refreshTokens, httpSession.getRefreshTokens());
        assertNotSame(refreshTokens, httpSession.getRefreshTokens());
    }
}
