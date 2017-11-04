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

package net.krotscheck.kangaroo.authz.common.util;


import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;

import static org.bouncycastle.crypto.PBEParametersGenerator.PKCS5PasswordToBytes;

/**
 * Password hashing and comparison utilities.
 *
 * @author Michael Krotscheck
 */
public final class PasswordUtil {

    /**
     * Random generator.
     */
    private static final DigestRandomGenerator GENERATOR =
            new DigestRandomGenerator(new SHA3Digest(512));

    /**
     * Private constructor for a utility class.
     */
    private PasswordUtil() {

    }

    /**
     * Create a new salt using Java's SecureRandom for good entropy.
     *
     * @return A 32-byte-sized salt.
     */
    public static String createSalt() {
        byte[] salt = new byte[32];
        GENERATOR.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }

    /**
     * Hash a password.
     *
     * @param password The password.
     * @param salt     The salt.
     * @return The hash.
     */
    public static String hash(final String password, final String salt) {
        Integer keyLength = 512;
        Integer iterations = 101501;
        byte[] saltBytes = Base64.decodeBase64(salt);
        byte[] passBytes = PKCS5PasswordToBytes(password.toCharArray());

        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(passBytes, saltBytes, iterations);

        KeyParameter keys = (KeyParameter)
                generator.generateDerivedParameters(keyLength);

        byte[] encodedPassword = keys.getKey();
        return Base64.encodeBase64String(encodedPassword);
    }

    /**
     * Test whether a password/salt combination match a hash.
     *
     * @param password The password.
     * @param salt     The salt.
     * @param hashed   The hash to compare them against.
     * @return True if they match, otherwise false.
     */
    public static Boolean isValid(final String password,
                                  final String salt,
                                  final String hashed) {
        return hash(password, salt).equals(hashed);
    }
}
