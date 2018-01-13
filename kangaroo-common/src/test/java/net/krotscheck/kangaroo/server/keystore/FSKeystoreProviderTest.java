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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Unit test for the filesystem keystore loader.
 *
 * @author Michael Krotscheck
 */
public final class FSKeystoreProviderTest {

    /**
     * Test keystore.
     */
    private static final String KS_PATH =
            "src/test/resources/ssl/test_keystore.p12";

    /**
     * Password for the test keystore.
     */
    private static final String KS_PASS = "kangaroo";

    /**
     * Type for the test keystore.
     */
    private static final String KS_TYPE = "PKCS12";

    /**
     * Assert that we can successfully load the file.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testLoadFile() throws Exception {
        FSKeystoreProvider provider =
                new FSKeystoreProvider(KS_PATH, KS_PASS, KS_TYPE);
        KeyStore ks = provider.getKeyStore();
        Assert.assertEquals("kangaroo", ks.aliases().nextElement());

        Certificate[] chain = ks.getCertificateChain("kangaroo");
        Assert.assertEquals(1, chain.length);

        X509Certificate cert = (X509Certificate) chain[0];
        Assert.assertNotNull(cert);

        Principal iss = cert.getIssuerDN();
        Principal sub = cert.getSubjectDN();

        Assert.assertEquals("EMAILADDRESS=krotscheck@gmail.com, "
                        + "CN=localhost, OU=Kangaroo, O=Kangaroo, L=Seattle, "
                        + "ST=Washington, C=US",
                iss.getName());
        Assert.assertEquals("EMAILADDRESS=krotscheck@gmail.com, "
                        + "CN=localhost, OU=Kangaroo, O=Kangaroo, L=Seattle, "
                        + "ST=Washington, C=US",
                sub.getName());
    }

    /**
     * Assert that the keystore is a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getKeystoreSingleton() throws Exception {
        FSKeystoreProvider provider =
                new FSKeystoreProvider(KS_PATH, KS_PASS, KS_TYPE);
        KeyStore ks1 = provider.getKeyStore();
        KeyStore ks2 = provider.getKeyStore();
        Assert.assertSame(ks1, ks2);
    }

    /**
     * Assert that it errors on a nonexistent file.
     */
    @Test(expected = RuntimeException.class)
    public void testLoadNonexistentFile() {
        FSKeystoreProvider provider =
                new FSKeystoreProvider("", KS_PASS, KS_TYPE);
        provider.getKeyStore();
    }

    /**
     * Assert that it fails on a wrong password.
     */
    @Test(expected = RuntimeException.class)
    public void testLoadBadPassword() {
        FSKeystoreProvider provider =
                new FSKeystoreProvider(KS_PATH, "invalid", KS_TYPE);
        provider.getKeyStore();
    }

    /**
     * Assert that it fails on a bad store type.
     */
    @Test(expected = RuntimeException.class)
    public void testLoadBadType() {
        FSKeystoreProvider provider =
                new FSKeystoreProvider(KS_PATH, KS_PASS, "INVALID");
        provider.getKeyStore();
    }

    /**
     * Assert that the keystore is a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testWriteTo() throws Exception {
        FSKeystoreProvider provider =
                new FSKeystoreProvider(KS_PATH, KS_PASS, KS_TYPE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        provider.writeTo(baos);
        ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(bais, "kangaroo".toCharArray());
        Assert.assertEquals("kangaroo", keyStore.aliases().nextElement());
    }

    /**
     * Assert that, should an exception be thrown during storage, that it is
     * recast as a runtime exception.
     *
     * @throws Exception Thrown because RSA is not available.
     */
    @Test(expected = RuntimeException.class)
    public void testRecastExceptionWriteTo() throws Exception {
        FSKeystoreProvider provider = Mockito
                .spy(new FSKeystoreProvider(KS_PATH, KS_PASS, KS_TYPE));
        KeyStore mockStore = Mockito.mock(KeyStore.class);
        doThrow(Exception.class).when(mockStore).store(any(), any());

        Mockito.when(provider.getKeyStore()).thenReturn(mockStore);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        provider.writeTo(baos);
    }
}
