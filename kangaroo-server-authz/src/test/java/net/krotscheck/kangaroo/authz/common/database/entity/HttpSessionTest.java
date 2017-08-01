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
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

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
        httpSession.setCreationTime(1000);

        Assert.assertEquals(-1, httpSession.getSessionTimeout());
        httpSession.setSessionTimeout(1000);

        httpSession.setTimestamp(2000);

        Assert.assertNull(httpSession.getId());

        Session s = getSession();
        s.beginTransaction();
        s.save(httpSession);
        s.getTransaction().commit();

        Assert.assertNotNull(httpSession.getId());
        Assert.assertEquals(httpSession.getIdInternal(),
                httpSession.getId().toString(16));
        Assert.assertEquals(1000, httpSession.getCreationTime());
        Assert.assertEquals(1000, httpSession.getSessionTimeout());
        Assert.assertEquals(2000, httpSession.getTimestamp());
    }

    /**
     * Assert that modifying the isValid parameter works as expected.
     */
    @Test
    public void testIsValid() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        now.add(Calendar.SECOND, -500);

        HttpSession httpSession = new HttpSession();
        httpSession.setCreationTime(now.getTimeInMillis());
        httpSession.setTimestamp(now.getTimeInMillis());
        httpSession.setSessionTimeout(1000);

        Assert.assertTrue(httpSession.isValid());

        httpSession.setValid(false);
        Assert.assertFalse(httpSession.isValid());

        httpSession.setValid(true);
        Assert.assertTrue(httpSession.isValid());
    }

    /**
     * Assert that the isNew parameter works as expected.
     */
    @Test
    public void testIsNew() {
        HttpSession httpSession = new HttpSession();
        Assert.assertFalse(httpSession.isNew());
        httpSession.setNew(true);
        Assert.assertTrue(httpSession.isNew());
        httpSession.setNew(false);
        Assert.assertFalse(httpSession.isNew());
    }

    /**
     * Assert that assertSetAttribute throws.
     */
    @Test(expected = NotImplementedException.class)
    public void assertSetAttribute() {
        HttpSession httpSession = new HttpSession();
        httpSession.setAttribute("test", "value");
    }

    /**
     * Assert that assertGetAttribute throws.
     */
    @Test(expected = NotImplementedException.class)
    public void assertGetAttribute() {
        HttpSession httpSession = new HttpSession();
        httpSession.getAttribute("test");
    }

    /**
     * Assert that assertSetAttribute throws.
     */
    @Test(expected = NotImplementedException.class)
    public void assertRemoveAttribute() {
        HttpSession httpSession = new HttpSession();
        httpSession.removeAttribute("test");
    }

    /**
     * Assert get refresh tokens.
     */
    @Test
    public void assertGetSetRefreshTokens() {
        HttpSession httpSession = new HttpSession();
        List<OAuthToken> refreshTokens = new ArrayList<>();
        refreshTokens.add(new OAuthToken());

        Assert.assertEquals(0, httpSession.getRefreshTokens().size());
        httpSession.setRefreshTokens(refreshTokens);
        Assert.assertEquals(refreshTokens, httpSession.getRefreshTokens());
        Assert.assertNotSame(refreshTokens, httpSession.getRefreshTokens());
    }

    /**
     * Assert get id.
     */
    @Test
    public void assertGetSetId() {
        HttpSession httpSession = new HttpSession();

        Assert.assertNull(httpSession.getId());

        BigInteger id = BigInteger.TEN;
        httpSession.setId(id);
        Assert.assertEquals(id, httpSession.getId());
    }

    /**
     * Assert calling the access() method updates the timestamp.
     */
    @Test
    public void assertAccess() {
        HttpSession httpSession = new HttpSession();
        httpSession.setNew(true);
        httpSession.setTimestamp(2000);
        httpSession.access();

        Assert.assertEquals(httpSession.getTimestamp(),
                System.currentTimeMillis());
        Assert.assertFalse(httpSession.isNew());
    }
}
