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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * This is our 'fallback' ssl certificate provider, which uses the
 * BouncyCastle library to generate a certificate at runtime. This class is
 * provided to enforce a "Secure by default" implementation for our services.
 *
 * @author Michael Krotscheck
 */
public final class GeneratedKeystoreProvider implements IKeystoreProvider {

    /**
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(GeneratedKeystoreProvider.class);

    /**
     * Certificate name for our self-generated cert.
     */
    private static final X500Name X_500_NAME = new X500Name("C=US,"
            + "ST=Washington,"
            + "L=Seattle,"
            + "O=Kangaroo,"
            + "OU=Kangaroo,"
            + "CN=localhost");

    /**
     * Certificate serial number.
     */
    private static final BigInteger SERIAL =
            BigInteger.valueOf(new SecureRandom().nextInt());

    /**
     * The date before which the certificate is not valid.
     */
    private static final Date NOT_BEFORE = new Date(System.currentTimeMillis()
            - 1000L * 60 * 60 * 24 * 30); // One month ago.

    /**
     * The date after which the certificate is not valid.
     */
    private static final Date NOT_AFTER = new Date(System.currentTimeMillis()
            + (1000L * 60 * 60 * 24 * 365 * 10)); // Ten years from now.

    /**
     * Password used to create the keystore.
     */
    private final String keystorePass;

    /**
     * Password for the certificate's private key.
     */
    private final String certificatePass;

    /**
     * Password for the certificate alias.
     */
    private final String alias;

    /**
     * The keypair used for signing certificates.
     */
    private KeyPair keyPair;

    /**
     * Certificate builder.
     */
    private X509v3CertificateBuilder certificateBuilder;

    /**
     * Certificate signer.
     */
    private ContentSigner certificateSigner;

    /**
     * The generated certificate chain.
     */
    private Certificate[] chain;

    /**
     * The generated keystore.
     */
    private KeyStore keyStore;

    /**
     * Create a new instance of the keystore provider.
     */
    public GeneratedKeystoreProvider() {
        this("kangaroo", "kangaroo", "kangaroo");
    }

    /**
     * Create a new instance of the keystore provider.
     *
     * @param keystorePass    A password for the keystore.
     * @param certificatePass A password for the cert private key.
     * @param alias           A certificate alias.
     */
    public GeneratedKeystoreProvider(final String keystorePass,
                                     final String certificatePass,
                                     final String alias) {
        this.keystorePass = keystorePass;
        this.certificatePass = certificatePass;
        this.alias = alias;
    }

    /**
     * Retrieve the keypair.
     *
     * @return The keypair for this generator.
     * @throws NoSuchAlgorithmException Thrown if the RSA keygen alg is not
     *                                  available.
     */
    public KeyPair getKeyPair() throws NoSuchAlgorithmException {
        if (keyPair == null) {
            logger.info("Generating Keypair");
            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024 * 2);
            keyPair = keyPairGenerator.generateKeyPair();
        }
        return keyPair;
    }

    /**
     * Get the certificate builder for this generator.
     *
     * @return A certificate builder, using the public key.
     * @throws NoSuchAlgorithmException Thrown if the RSA keygen alg is not
     *                                  available.
     */
    public X509v3CertificateBuilder getCertificateBuilder()
            throws NoSuchAlgorithmException {
        if (certificateBuilder == null) {
            logger.info("Generating Certificate Builder");
            byte[] publicKey = getKeyPair().getPublic().getEncoded();
            SubjectPublicKeyInfo pki =
                    SubjectPublicKeyInfo.getInstance(publicKey);
            certificateBuilder = new X509v3CertificateBuilder(
                    X_500_NAME, SERIAL, NOT_BEFORE, NOT_AFTER, X_500_NAME, pki);
        }
        return certificateBuilder;
    }

    /**
     * Get the certificate builder for this generator.
     *
     * @return A certificate builder, using the public key.
     * @throws NoSuchAlgorithmException  Thrown if the RSA keygen alg is not
     *                                   available.
     * @throws IOException               Thrown if the generated key cannot be
     *                                   read.
     * @throws OperatorCreationException Thrown if we cannot create a content
     *                                   signer.
     */
    public ContentSigner getCertificateSigner() throws NoSuchAlgorithmException,
            IOException, OperatorCreationException {
        if (certificateSigner == null) {
            logger.info("Generating Certificate Signer");

            // Create the certificate signer.
            byte[] privateKey = getKeyPair().getPrivate().getEncoded();
            AlgorithmIdentifier sigAlgId =
                    new DefaultSignatureAlgorithmIdentifierFinder()
                            .find("SHA256WithRSAEncryption");
            AlgorithmIdentifier digAlgId =
                    new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            certificateSigner =
                    new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                            .build(PrivateKeyFactory.createKey(privateKey));
        }
        return certificateSigner;
    }

    /**
     * Retrieve (and/or create) the certificate and its chain.
     *
     * @return The full certificate chain.
     * @throws IOException               Thrown if the keypair cannot be
     *                                   read.
     * @throws OperatorCreationException Thrown if the signer cannot be created.
     * @throws NoSuchAlgorithmException  Thrown if the keypair cannot be
     *                                   generated.
     * @throws CertificateException      Thrown if the certificate cannot be
     *                                   read.
     */
    public Certificate[] getCertificates() throws IOException,
            NoSuchAlgorithmException, OperatorCreationException,
            CertificateException {
        if (chain == null) {
            logger.info("Generating x509 Certificate");
            ContentSigner signer = getCertificateSigner();
            X509v3CertificateBuilder builder = getCertificateBuilder();

            X509CertificateHolder holder = builder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter()
                    .getCertificate(holder);
            chain = new Certificate[]{cert};
        }
        return chain;
    }

    /**
     * Retrieve the key from this provider.
     *
     * @return The private key.
     * @throws NoSuchAlgorithmException Thrown if the keypair cannot be
     *                                  generated.
     */
    public PrivateKey getKey() throws NoSuchAlgorithmException {
        return getKeyPair().getPrivate();
    }

    /**
     * Retrieve the keystore from this provider.
     *
     * @return The private key.
     */
    public KeyStore getKeyStore() {
        if (keyStore == null) {
            try {
                // Build a new keystore.
                keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, this.keystorePass.toCharArray());

                Key key = getKey();
                Certificate[] chain = getCertificates();

                // Add the certificate and the key
                keyStore.setKeyEntry(alias,
                        key,
                        this.certificatePass.toCharArray(),
                        chain);
            } catch (Exception kse) {
                throw new RuntimeException(kse);
            }
        }
        return keyStore;
    }

    /**
     * Store the keystore to a provided output stream.
     *
     * @param outputStream The output stream to store to.
     */
    @Override
    public void writeTo(final OutputStream outputStream) {
        try {
            getKeyStore().store(outputStream, keystorePass.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
