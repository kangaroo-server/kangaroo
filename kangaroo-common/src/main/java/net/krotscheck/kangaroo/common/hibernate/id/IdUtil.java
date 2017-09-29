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

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * A small utility that generates positive database identifiers.
 *
 * @author Michael Krotscheck
 */
public final class IdUtil {

    /**
     * Static ID util, for convenience access.
     */
    private static final IdUtil INSTANCE = new IdUtil();

    /**
     * Our Base16 encoder/decoder.
     */
    private static final BaseEncoding BASE_16 = BaseEncoding.base16();

    /**
     * The # of bytes in the ID. OSSTP recommends 128 bits, which means 16
     * bytes.
     */
    private static final int BYTE_COUNT = 16;

    /**
     * Random number generator.
     */
    private Random randomNumberGenerator;

    /**
     * Utility class, private constructor.
     */
    protected IdUtil() {
        try {
            randomNumberGenerator = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    /**
     * Convert an ID, represented as a string, to a BigInteger id.
     *
     * @param idString The string.
     * @return The BigInteger representation of that ID.
     */
    public static byte[] fromString(final String idString) {
        if (Strings.isNullOrEmpty(idString)) {
            return null;
        }
        byte[] decoded = BASE_16.decode(idString);
        if (decoded == null || decoded.length != BYTE_COUNT) {
            throw new IllegalArgumentException("id is not the correct length");
        }
        return decoded;
    }

    /**
     * Convert a bigInteger to an string ID representation.
     *
     * @param id The ID to convert.
     * @return The string representation of this id.
     */
    public static String toString(final byte[] id) {
        if (id == null) {
            return null;
        }
        if (id.length != BYTE_COUNT) {
            throw new IllegalArgumentException("id is not the correct length");
        }
        return BASE_16.encode(id);
    }

    /**
     * Generate an ID.
     *
     * @return An ID generated from the SecureRandom stream of bytes.
     */
    public static byte[] next() {
        return INSTANCE.nextInternal();
    }

    /**
     * Generate an ID.
     *
     * @return An ID generated from the SecureRandom stream of bytes.
     */
    protected byte[] nextInternal() {
        byte[] randomBytes = new byte[BYTE_COUNT];
        randomNumberGenerator.nextBytes(randomBytes);
        return randomBytes;
    }
}
