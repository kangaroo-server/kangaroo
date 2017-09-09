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
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

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

        Assert.assertEquals(-1, httpSession.getSessionTimeout());
        httpSession.setSessionTimeout(1000);

        Assert.assertNull(httpSession.getId());

        Session s = getSession();
        s.beginTransaction();
        s.save(httpSession);
        s.getTransaction().commit();

        Assert.assertNotNull(httpSession.getId());
        Assert.assertEquals(httpSession.getModifiedDate(),
                httpSession.getCreatedDate());
        Assert.assertEquals(1000, httpSession.getSessionTimeout());
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
     * Test created date get/set.
     */
    @Test
    public void testGetSetCreatedDate() {
        HttpSession a = new HttpSession();
        Calendar d = Calendar.getInstance();

        Assert.assertNull(a.getCreatedDate());
        a.setCreatedDate(d);
        Assert.assertEquals(d, a.getCreatedDate());
        Assert.assertNotSame(d, a.getCreatedDate());
    }

    /**
     * Test created date get/set.
     */
    @Test
    public void testGetSetModifiedDate() {
        HttpSession a = new HttpSession();
        Calendar d = Calendar.getInstance();

        Assert.assertNull(a.getModifiedDate());
        a.setModifiedDate(d);
        Assert.assertEquals(d, a.getModifiedDate());
        Assert.assertNotSame(d, a.getModifiedDate());
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
     * Test Equality by ID.
     */
    @Test
    public void testEquality() {
        BigInteger id = new BigInteger(10, new Random());
        BigInteger id2 = new BigInteger(10, new Random());

        HttpSession a = new HttpSession();
        a.setId(id);

        HttpSession b = new HttpSession();
        b.setId(id);

        HttpSession c = new HttpSession();
        c.setId(id2);

        HttpSession d = new HttpSession();

        Object e = new Object();

        Assert.assertTrue(a.equals(a));
        Assert.assertFalse(a.equals(null));
        Assert.assertFalse(a.equals(e));
        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertFalse(a.equals(c));
        Assert.assertFalse(c.equals(a));
        Assert.assertFalse(a.equals(d));
        Assert.assertFalse(d.equals(a));
    }

    /**
     * Test Equality by hashCode.
     */
    @Test
    public void testHashCode() {
        BigInteger id = new BigInteger(10, new Random());
        BigInteger id2 = new BigInteger(10, new Random());

        HttpSession a = new HttpSession();
        a.setId(id);

        HttpSession b = new HttpSession();
        b.setId(id);

        HttpSession c = new HttpSession();
        c.setId(id2);

        HttpSession d = new HttpSession();

        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertNotEquals(a.hashCode(), c.hashCode());
        Assert.assertNotEquals(a.hashCode(), d.hashCode());
    }

    /**
     * Test toString.
     */
    @Test
    public void testToString() {
        BigInteger id = new BigInteger(10, new Random());
        HttpSession a = new HttpSession();
        a.setId(id);
        HttpSession b = new HttpSession();

        Assert.assertEquals(
                String.format("net.krotscheck.kangaroo.authz.common.database"
                                + ".entity.HttpSession [id=%s]",
                        a.getId()),
                a.toString());
        Assert.assertEquals("net.krotscheck.kangaroo.authz"
                + ".common.database.entity.HttpSession"
                + " [id=null]", b.toString());
    }

    /**
     * Test cloneable.
     *
     * @throws CloneNotSupportedException Should not be thrown.
     */
    @Test
    public void testCloneable() throws CloneNotSupportedException {
        BigInteger id = new BigInteger(10, new Random());
        HttpSession a = new HttpSession();
        a.setId(id);
        HttpSession b = (HttpSession) a.clone();

        Assert.assertEquals(a.getId(), b.getId());
    }
}
