/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.features.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing and comparison utilities.
 *
 * @author Michael Krotscheck
 */
public final class PasswordUtil {

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
    public static byte[] createSalt() {
        SecureRandom srand = new SecureRandom();
        byte[] salt = new byte[32];
        srand.nextBytes(salt);
        return salt;
    }

    /**
     * Hash a password.
     *
     * @param password The password.
     * @param salt     The salt.
     * @return The hash.
     * @throws NoSuchAlgorithmException Thrown if the encryption algorithm is
     *                                  not available.
     * @throws InvalidKeySpecException  Thrown if the key spec is not
     *                                  available.
     */
    public static byte[] hash(final String password,
                              final byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] chars = password.toCharArray();
        int iterations = 1000;
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);

        SecretKeyFactory skf =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
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
                                  final byte[] salt,
                                  final byte[] hashed) {
        try {
            return Arrays.equals(hash(password, salt), hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
