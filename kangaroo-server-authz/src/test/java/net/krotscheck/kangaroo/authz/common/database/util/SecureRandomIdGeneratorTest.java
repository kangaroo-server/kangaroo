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

package net.krotscheck.kangaroo.authz.common.database.util;

import org.hibernate.id.IdentifierGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * This hibernate ID generator, uses the SecureRandom class to generate
 * 64-byte random ID's.
 *
 * @author Michael Krotscheck
 */
public final class SecureRandomIdGeneratorTest {

    /**
     * Assert that we only ever generate positive ID's.
     */
    @Test
    public void testOnlyPositives() {
        IdentifierGenerator generator = new SecureRandomIdGenerator();

        // Statistically speaking, 5 of these should be negative.
        for (int i = 0; i < 10; i++) {
            BigInteger result =
                    (BigInteger) generator.generate(null, null);
            Assert.assertEquals(1, result.compareTo(BigInteger.ZERO));
        }
    }

    /**
     * Assert that the default byte size is observed.
     */
    @Test
    public void testDefaultByteLength() {
        SecureRandomIdGenerator generator = new SecureRandomIdGenerator();
        BigInteger result =
                (BigInteger) generator.generate(null, null);
        byte[] bytes = result.toByteArray();
        Assert.assertEquals(64, generator.getByteCount());
        Assert.assertEquals(generator.getByteCount(), bytes.length);
    }

    /**
     * Assert that we can control the byte size.
     */
    @Test
    public void testByteLength() {
        SecureRandomIdGenerator generator = new SecureRandomIdGenerator();
        generator.setByteCount(20);
        BigInteger result =
                (BigInteger) generator.generate(null, null);
        byte[] bytes = result.toByteArray();
        Assert.assertEquals(generator.getByteCount(), bytes.length);
    }
}
