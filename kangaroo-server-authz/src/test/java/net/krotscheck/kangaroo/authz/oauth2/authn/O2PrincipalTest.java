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
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.junit.Test;

import javax.security.auth.Subject;
import java.security.Principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the O2 principal, which contains all bits necessary to
 * evaluate the security credentials of a specific request. It's built up by
 * a series of filters which each evaluate, and attempt to merge, the discovered
 * security context into the current request's context. It's up to the
 * principal to determine if it may be merged.
 *
 * @author Michael Krotscheck
 */
public final class O2PrincipalTest {

    /**
     * Assert the behavior of the default constructor.
     */
    @Test
    public void testDefaultConstructor() {
        O2Principal p = new O2Principal();

        assertEquals(O2AuthScheme.None.toString(), p.getScheme());
        assertNull(p.getName());
        assertFalse(p.implies(mock(Subject.class)));
        assertNull(p.getContext());
    }

    /**
     * Assert that we can create a principal with a public client.
     */
    @Test
    public void testConstructorPublicClient() {
        Client testClient = new Client();
        testClient.setId(IdUtil.next());
        testClient.setClientSecret(null);
        testClient.setName("Test client");
        O2Principal p = new O2Principal(testClient);

        assertEquals(O2AuthScheme.ClientPublic.toString(), p.getScheme());
        assertEquals(testClient.getName(), p.getName());
        assertFalse(p.implies(mock(Subject.class)));
        assertEquals(testClient, p.getContext());
    }

    /**
     * Assert that we can create a principal with a private client.
     */
    @Test
    public void testConstructorPrivateClient() {
        Client testClient = new Client();
        testClient.setId(IdUtil.next());
        testClient.setClientSecret(IdUtil.toString(IdUtil.next()));
        testClient.setName("Test client");
        O2Principal p = new O2Principal(testClient);

        assertEquals(O2AuthScheme.ClientPrivate.toString(), p.getScheme());
        assertEquals(testClient.getName(), p.getName());
        assertFalse(p.implies(mock(Subject.class)));
        assertEquals(testClient, p.getContext());
    }

    /**
     * Assert that we can create a principal with a null client.
     */
    @Test
    public void testConstructorNullClient() {
        O2Principal p = new O2Principal(null);

        assertEquals(O2AuthScheme.None.toString(), p.getScheme());
        assertNull(p.getName());
        assertFalse(p.implies(mock(Subject.class)));
        assertNull(p.getContext());
    }

    /**
     * Assert that we can create a principal with a client that has no values
     * set.
     */
    @Test
    public void testConstructorInvalidClient() {
        Client testClient = new Client();
        O2Principal p = new O2Principal(testClient);

        assertEquals(O2AuthScheme.None.toString(), p.getScheme());
        assertNull(p.getName());
        assertFalse(p.implies(mock(Subject.class)));
        assertNull(p.getContext());
    }

    /**
     * Test valid cases in the equality checker. The required functionality here
     * is  that of a safe merge - in other words, if A === B, or if A && B ==
     * null, or if A == null && B. If A && B && A !== B, this should throw.
     */
    @Test
    public void testSameOrOne() {
        O2Principal p = new O2Principal();
        Client left = new Client();
        left.setId(IdUtil.next());
        Client right = new Client();
        right.setId(left.getId());

        assertEquals(left, p.sameOrOne(left, null));
        assertEquals(right, p.sameOrOne(null, right));
        assertEquals(left, p.sameOrOne(left, right));
    }

    /**
     * If A && B && A !== B, this should return null.
     */
    @Test(expected = AccessDeniedException.class)
    public void testSameOrOneFail() {
        O2Principal p = new O2Principal();
        Client left = new Client();
        left.setId(IdUtil.next());
        Client right = new Client();
        right.setId(IdUtil.next());

        p.sameOrOne(left, right);
    }

    /**
     * Assert that attempting to merge a null value returns the principal.
     */
    @Test
    public void testMergeNullInput() {
        Client c = new Client();
        c.setId(IdUtil.next());

        O2Principal principal = new O2Principal(c);
        O2Principal merged = principal.merge(null);
        assertEquals(principal, merged);
        assertNotSame(principal, merged);
    }

    /**
     * Assert that merging with a principal that's not an O2Principal returns
     * the original principal.
     */
    @Test
    public void testMergeWrongPrincipalInput() {
        Client c = new Client();
        c.setId(IdUtil.next());

        O2Principal principal = new O2Principal(c);
        Principal mergeable = () -> null;

        O2Principal merged = principal.merge(mergeable);
        assertEquals(principal, merged);
        assertNotSame(principal, merged);
    }

    /**
     * Try to merge two principals that are backed by different clients.
     */
    @Test(expected = AccessDeniedException.class)
    public void testMergeMismatchClient() {
        Client c1 = new Client();
        c1.setId(IdUtil.next());
        O2Principal p1 = new O2Principal(c1);

        Client c2 = new Client();
        c2.setId(IdUtil.next());
        O2Principal p2 = new O2Principal(c2);

        p1.merge(p2);
    }

    /**
     * Try to merge two clients which present as the same, but have different
     * auth schemes (which shouldn't happen, but let's make sure we test for
     * it).
     */
    @Test(expected = AccessDeniedException.class)
    public void testMergeTwoAuthSchemes() {
        Client c1 = new Client();
        c1.setId(IdUtil.next());
        c1.setClientSecret("foo");
        O2Principal p1 = new O2Principal(c1);

        Client c2 = new Client();
        c2.setId(c1.getId());
        O2Principal p2 = new O2Principal(c2);

        p1.merge(p2);
    }

    /**
     * Test merging two clients that have the same auth scheme (private auth
     * via header OR body for example).
     */
    @Test
    public void testMergeSameAuthScheme() {
        Client c1 = new Client();
        c1.setId(IdUtil.next());
        c1.setClientSecret("secret");
        O2Principal p1 = new O2Principal(c1);

        Client c2 = new Client();
        c2.setId(c1.getId());
        c2.setClientSecret("secret");
        O2Principal p2 = new O2Principal(c2);

        O2Principal merged = p1.merge(p2);
        assertEquals(p1, merged);
        assertEquals(p2, merged);
        assertNotSame(p1, merged);
        assertNotSame(p2, merged);
    }

    /**
     * Try to merge a private principal with no auth scheme.
     */
    @Test
    public void testMergeNoAuthSchemePrivate() {
        Client c = new Client();
        c.setId(IdUtil.next());
        c.setClientSecret("secret");
        O2Principal p1 = new O2Principal(c);

        O2Principal p2 = new O2Principal();

        O2Principal merged = p1.merge(p2);
        assertEquals(p1, merged);
        assertNotSame(p1, merged);
    }

    /**
     * Try to merge principals with no auth scheme.
     */
    @Test
    public void testMergeNoAuthScheme() {
        O2Principal p1 = new O2Principal();
        O2Principal p2 = new O2Principal();

        O2Principal merged = p1.merge(p2);
        assertEquals(p1, merged);
        assertNotSame(p1, merged);

        O2Principal mergedReverse = p2.merge(p1);
        assertEquals(p1, mergedReverse);
        assertNotSame(p1, mergedReverse);
    }

    /**
     * Test the equality operator.
     */
    @Test
    public void testEquality() {
        Client c1 = new Client();
        c1.setId(IdUtil.next());
        c1.setClientSecret("secret");
        O2Principal p1 = new O2Principal(c1);
        O2Principal pSame = new O2Principal(c1);

        Client c2 = new Client();
        c2.setId(IdUtil.next());
        O2Principal p2 = new O2Principal(c2);

        Client c3 = new Client();
        c3.setId(IdUtil.next());
        O2Principal p3 = new O2Principal(c3);

        assertFalse(p1.equals(new Object()));
        assertFalse(p1.equals(null));
        assertFalse(p1.equals(p2));
        assertFalse(p1.equals(p2));
        assertFalse(p1.equals(p3));
        assertFalse(p2.equals(p3));

        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(pSame));
    }

    /**
     * Test hashcode generation.
     */
    @Test
    public void testHashCode() {
        Client c1 = new Client();
        c1.setId(IdUtil.next());
        c1.setClientSecret("secret");
        O2Principal p1 = new O2Principal(c1);

        Client cSame = new Client();
        cSame.setId(c1.getId());
        cSame.setClientSecret(c1.getClientSecret());
        O2Principal pSame = new O2Principal(cSame);

        Client c2 = new Client();
        c2.setId(IdUtil.next());
        c2.setClientSecret("other_secret");
        O2Principal p2 = new O2Principal(c2);

        assertEquals(p1.hashCode(), pSame.hashCode());
        assertNotEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(pSame.hashCode(), p2.hashCode());
    }
}
