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

package net.krotscheck.kangaroo.server.keystore;

import net.krotscheck.kangaroo.test.rule.WorkingDirectoryRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Unit test for our internal keystore provider, used when an external
 * keystore has not been configured.
 *
 * @author Michael Krotscheck
 */
public final class GeneratedKeystoreProviderTest {

    /**
     * Ensure that we have a working directory.
     */
    @Rule
    public final WorkingDirectoryRule workingDirectory =
            new WorkingDirectoryRule();

    /**
     * Generate the keystore.
     */
    private GeneratedKeystoreProvider provider;

    /**
     * Setup the tests.
     */
    @Before
    public void setUp() {
        File tempDir = workingDirectory.getWorkingDir();
        provider = Mockito.spy(new GeneratedKeystoreProvider(tempDir
                .getAbsolutePath()));
    }

    /**
     * Assert that a new keystore is generated in the working directory if
     * it doesn't exist yet.
     */
    @Test
    public void testGeneratedFile() {
        File tempDir = workingDirectory.getWorkingDir();
        provider.getKeyStore(); // Build the keystore.

        File generatedKeystore = new File(tempDir, "generated.p12");
        assertTrue(generatedKeystore.exists());
        assertTrue(generatedKeystore.isFile());
    }

    /**
     * Assert that an existing keystore is used if found in the working
     * directory.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testReuseKeystore() throws Exception {
        File tempDir = workingDirectory.getWorkingDir();
        provider.getKeyStore(); // Build the keystore.

        // Create a new keystore impl.
        GeneratedKeystoreProvider provider2
                = new GeneratedKeystoreProvider(tempDir.getAbsolutePath());

        RSAPrivateKey key1 = (RSAPrivateKey) provider.getKeyStore()
                .getKey("kangaroo", "kangaroo".toCharArray());

        RSAPrivateKey key2 = (RSAPrivateKey) provider2.getKeyStore()
                .getKey("kangaroo", "kangaroo".toCharArray());

        assertEquals(key1.getModulus(), key2.getModulus());
    }

    /**
     * Assert that failing to access the keystore in the working directory
     * will cause a runtime error.
     */

    /**
     * Assert that an existing keystore is used if found in the working
     * directory.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = RuntimeException.class)
    public void testCannotReuseKeystore() throws Exception {
        File tempDir = workingDirectory.getWorkingDir();
        // Create a new keystore impl.
        GeneratedKeystoreProvider provider2
                = new GeneratedKeystoreProvider(tempDir.getAbsolutePath(),
                "otherpassword", "otherpassword", "otheralias");
        provider2.getKeyStore();

        // This should throw.
        provider.getKeyStore();
    }


    /**
     * Assert that the chain contains the certificates we're expecting.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void validateCertificates() throws Exception {
        Certificate[] chain =
                provider.getKeyStore().getCertificateChain("kangaroo");
        assertEquals(1, chain.length);

        X509Certificate cert = (X509Certificate) chain[0];
        assertNotNull(cert);

        Principal iss = cert.getIssuerDN();
        Principal sub = cert.getSubjectDN();

        assertEquals("CN=localhost, OU=Kangaroo, O=Kangaroo, "
                        + "L=Seattle, ST=Washington, C=US",
                iss.getName());
        assertEquals("CN=localhost, OU=Kangaroo, O=Kangaroo, "
                        + "L=Seattle, ST=Washington, C=US",
                sub.getName());
    }

    /**
     * Assert that the key is valid, and can be used to verify the cert.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getKey() throws Exception {
        KeyStore ks = provider.getKeyStore();
        RSAPrivateKey key = (RSAPrivateKey)
                ks.getKey("kangaroo", "kangaroo".toCharArray());
        Certificate[] chain =
                provider.getKeyStore().getCertificateChain("kangaroo");
        assertEquals(1, chain.length);
        RSAPublicKey pubKey = (RSAPublicKey) chain[0].getPublicKey();

        assertEquals(key.getModulus(), pubKey.getModulus());
    }

    /**
     * Assert that the keystore is a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getKeystoreSingleton() throws Exception {
        KeyStore ks1 = provider.getKeyStore();
        KeyStore ks2 = provider.getKeyStore();
        assertSame(ks1, ks2);
    }

    /**
     * Test that we can write to an output stream.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testWriteTo() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        provider.getKeyStore(); // Build the keystore.
        provider.writeTo(baos);
        ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(bais, "kangaroo".toCharArray());
        assertEquals("kangaroo", keyStore.aliases().nextElement());
    }

    /**
     * Assert that, should an exception be thrown during key generation, it
     * is recast as a runtime exception.
     *
     * @throws Exception Thrown because RSA is not available.
     */
    @Test(expected = RuntimeException.class)
    public void testRecastExceptionWriteTo() throws Exception {
        doThrow(new NoSuchAlgorithmException())
                .when(provider)
                .getCertificateBuilder(any());
        provider.getKeyStore();
    }
}
