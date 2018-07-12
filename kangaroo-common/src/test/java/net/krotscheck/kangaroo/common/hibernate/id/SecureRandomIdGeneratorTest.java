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

package net.krotscheck.kangaroo.common.hibernate.id;

import net.krotscheck.kangaroo.common.hibernate.entity.TestEntity;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * This hibernate ID id, uses the SecureRandom class to generate
 * 16-byte random ID's.
 *
 * @author Michael Krotscheck
 */
public final class SecureRandomIdGeneratorTest extends DatabaseTest {

    /**
     * Assert that using it via an annotated ID works.
     */
    @Test
    public void assertAnnotationPersistence() {
        TestEntity e = new TestEntity();
        e.setName("foo");

        Session s = getSession();
        s.beginTransaction();
        s.save(e);
        s.getTransaction().commit();

        assertNotNull(e);
        assertNotNull(e.getId());
    }

    /**
     * Assert that we can generate a new id.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertGenerateSimple() throws Exception {
    }

    /**
     * Assert that we can generate a new id if the first (or second, or
     * third) conflicts.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertGenerateSimpleNoConflict() throws Exception {
        SecureRandomIdGenerator generator = spy(new SecureRandomIdGenerator());
        doReturn(true, true, false)
                .when(generator)
                .hasDuplicate(any(), any());

        BigInteger id = (BigInteger) generator.generate(
                (SharedSessionContractImplementor) getSession(), null);

        assertNotNull(id);
        Mockito.verify(generator, times(3))
                .hasDuplicate(any(), any());
    }

    /**
     * Assert that duplicate check returns false on new ID.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertDuplicateFalseOnNoMatch() throws Exception {
        SecureRandomIdGenerator generator = new SecureRandomIdGenerator();
        generator.setSql("select count(id) from test where id=?");
        Boolean result = generator.hasDuplicate(
                (SharedSessionContractImplementor) getSession(),
                IdUtil.next());
        assertFalse(result);
    }

    /**
     * Assert that duplicate check closes everything on a failure.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertDuplicateTrueMatch() throws Exception {
        TestEntity e = new TestEntity();
        e.setName("foo");

        Session s = getSession();
        s.beginTransaction();
        s.save(e);
        s.getTransaction().commit();

        SecureRandomIdGenerator generator = new SecureRandomIdGenerator();
        generator.setSql("select count(id) from test where id=?");
        Boolean result = generator.hasDuplicate(
                (SharedSessionContractImplementor) getSession(),
                e.getId());
        assertTrue(result);
    }

    /**
     * Assert that an SQL exception is thrown if the result set returns nothing.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = IdentifierGenerationException.class)
    public void assertIdentifierExceptionOnMismatch() throws Exception {
        SecureRandomIdGenerator generator = new SecureRandomIdGenerator();

        // Mariadb can signal an error directly.
        String sql = "signal set message_text='an error occurred';";

        // This should return a null result set, causing an SQL Exception.
        generator.setSql(sql);
        generator.hasDuplicate(
                (SharedSessionContractImplementor) getSession(),
                null);
    }

    /**
     * Assert that duplicate check returns false with no result set.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertFalseOnNoResults() throws Exception {
        SecureRandomIdGenerator generator = new SecureRandomIdGenerator();

        // This should return a null result set, causing an SQL Exception.
        generator.setSql("select * from test where 1=?");

        Boolean result = generator.hasDuplicate(
                (SharedSessionContractImplementor) getSession(),
                null);
        assertFalse(result);
    }
}
