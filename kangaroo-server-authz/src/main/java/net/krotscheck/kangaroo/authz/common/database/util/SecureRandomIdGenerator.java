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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * This identity generator uses java's secureRandom to generate database
 * record ID's.
 *
 * @author Michael Krotscheck
 */
public final class SecureRandomIdGenerator implements IdentifierGenerator {

    /**
     * Random number generator.
     */
    private static final Random RND = new SecureRandom();

    /**
     * The # of bytes in the ID.
     */
    private int byteCount = 64;

    /**
     * Get the # of bytes in the generated ID.
     *
     * @return The # of bytes.
     */
    public int getByteCount() {
        return byteCount;
    }

    /**
     * Set the # of bytes in the ID.
     *
     * @param byteCount The new bytecount.
     */
    public void setByteCount(final int byteCount) {
        this.byteCount = byteCount;
    }

    /**
     * Generate a new identifier.
     *
     * @param session The session from which the request originates
     * @param object  the entity or collection (idbag) for which the id is being
     *                generated.
     * @return a new identifier
     * @throws HibernateException Indicates trouble generating the identifier
     */
    @Override
    public Serializable generate(final SharedSessionContractImplementor session,
                                 final Object object)
            throws HibernateException {
        byte[] randomBytes = new byte[byteCount];
        RND.nextBytes(randomBytes);
        BigInteger newId = new BigInteger(randomBytes);
        return newId.abs(); // Positive only.
    }
}
