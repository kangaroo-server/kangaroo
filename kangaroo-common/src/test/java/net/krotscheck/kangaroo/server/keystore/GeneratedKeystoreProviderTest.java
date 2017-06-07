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

import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Unit test for our internal keystore provider, used when an external
 * keystore has not been configured.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
public class GeneratedKeystoreProviderTest {

    /**
     * Assert that the chain contains the certificates we're expecting.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getChain() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        Certificate[] chain = provider.getCertificates();
        Assert.assertEquals(1, chain.length);

        X509Certificate cert = (X509Certificate) chain[0];
        Assert.assertNotNull(cert);

        Principal iss = cert.getIssuerDN();
        Principal sub = cert.getSubjectDN();

        Assert.assertEquals("CN=localhost, OU=Kangaroo, O=Kangaroo, "
                        + "L=Seattle, ST=Washington, C=US",
                iss.getName());
        Assert.assertEquals("CN=localhost, OU=Kangaroo, O=Kangaroo, "
                        + "L=Seattle, ST=Washington, C=US",
                sub.getName());
    }

    /**
     * Assert that the chain is a singleton constructor.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getChainSingleton() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        Certificate[] chain1 = provider.getCertificates();
        Certificate[] chain2 = provider.getCertificates();

        Assert.assertSame(chain1, chain2);
    }

    /**
     * Assert that the keypair method returns a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getKeyPairSingleton() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        KeyPair keyPair1 = provider.getKeyPair();
        KeyPair keyPair2 = provider.getKeyPair();

        Assert.assertSame(keyPair1, keyPair2);
    }

    /**
     * Assert that the signer method returns a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getSignerSingleton() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        ContentSigner signer1 = provider.getCertificateSigner();
        ContentSigner signer2 = provider.getCertificateSigner();

        Assert.assertSame(signer1, signer2);
    }

    /**
     * Assert that the buidler method returns a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getBuilderSingleton() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        X509v3CertificateBuilder builder1 = provider.getCertificateBuilder();
        X509v3CertificateBuilder builder2 = provider.getCertificateBuilder();

        Assert.assertSame(builder1, builder2);
    }

    /**
     * Assert that the key is valid, and can be used to verify the cert.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getKey() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        RSAPrivateKey key = (RSAPrivateKey) provider.getKey();
        RSAPublicKey pubKey = (RSAPublicKey) provider
                .getCertificates()[0].getPublicKey();

        Assert.assertEquals(key.getModulus(), pubKey.getModulus());
    }

    /**
     * Assert that the keystore can be created.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getKeystore() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        KeyStore ks = provider.getKeyStore();
        Assert.assertEquals("kangaroo", ks.aliases().nextElement());

        Certificate[] chain = ks.getCertificateChain("kangaroo");
        Assert.assertEquals(1, chain.length);

        X509Certificate cert = (X509Certificate) chain[0];
        Assert.assertNotNull(cert);

        Principal iss = cert.getIssuerDN();
        Principal sub = cert.getSubjectDN();

        Assert.assertEquals("CN=localhost, OU=Kangaroo, O=Kangaroo, "
                        + "L=Seattle, ST=Washington, C=US",
                iss.getName());
        Assert.assertEquals("CN=localhost, OU=Kangaroo, O=Kangaroo, "
                        + "L=Seattle, ST=Washington, C=US",
                sub.getName());
    }

    /**
     * Assert that the keystore is a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void getKeystoreSingleton() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        KeyStore ks1 = provider.getKeyStore();
        KeyStore ks2 = provider.getKeyStore();
        Assert.assertSame(ks1, ks2);
    }

    /**
     * Assert that the keystore is a singleton.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testWriteTo() throws Exception {
        GeneratedKeystoreProvider provider = new GeneratedKeystoreProvider();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        provider.writeTo(baos);
        ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(bais, "kangaroo".toCharArray());
        Assert.assertEquals("kangaroo", keyStore.aliases().nextElement());
    }

    /**
     * Assert that, should an exception be thrown during key generation, it
     * is recast as a runtime exception.
     *
     * @throws Exception Thrown because RSA is not available.
     */
    @Test(expected = RuntimeException.class)
    @PrepareOnlyThisForTest(GeneratedKeystoreProvider.class)
    public void testRecastExceptionGetKeyStore() throws Exception {
        GeneratedKeystoreProvider provider =
                PowerMockito.spy(new GeneratedKeystoreProvider());

        Mockito.when(provider.getKey())
                .thenThrow(new NoSuchAlgorithmException());

        provider.getKeyStore();
    }

    /**
     * Assert that, should an exception be thrown during key generation, it
     * is recast as a runtime exception.
     *
     * @throws Exception Thrown because RSA is not available.
     */
    @Test(expected = RuntimeException.class)
    @PrepareOnlyThisForTest(GeneratedKeystoreProvider.class)
    public void testRecastExceptionWriteTo() throws Exception {
        GeneratedKeystoreProvider provider =
                PowerMockito.spy(new GeneratedKeystoreProvider());

        Mockito.when(provider.getKey())
                .thenThrow(new NoSuchAlgorithmException());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        provider.writeTo(baos);
    }
}
