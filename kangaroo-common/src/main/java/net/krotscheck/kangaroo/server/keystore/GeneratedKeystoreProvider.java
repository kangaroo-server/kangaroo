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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * This is our 'fallback' ssl certificate provider, which uses the
 * BouncyCastle library to generate a certificate at runtime. This class is
 * provided to enforce a "Secure by default" implementation for our services.
 *
 * In order to persist certificates between runs, the generated certificate
 * will be stored on the filesystem in a predictable location, and reused
 * if found. If found, and inaccessible (different password, for example), the
 * process will terminate, as we don't accidentally want to delete something
 * that may have been put there intentionally.
 *
 * @author Michael Krotscheck
 */
public final class GeneratedKeystoreProvider implements IKeystoreProvider {

    /**
     * The keystore type.
     */
    private static final String KEYSTORE_TYPE = "PKCS12";

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
     * Logger instance.
     */
    private static Logger logger =
            LoggerFactory.getLogger(GeneratedKeystoreProvider.class);

    /**
     * The Filesystem keystore provider, which actually backs our external
     * contract.
     */
    private final FSKeystoreProvider generatedProvider;

    /**
     * The default keystore path, to which this keystore will be saved.
     */
    private final String keystorePath;

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
     * Create a new instance of the keystore provider.
     *
     * @param workingDirectory The working directory for the server.
     */
    public GeneratedKeystoreProvider(final String workingDirectory) {
        this(workingDirectory, "kangaroo", "kangaroo", "kangaroo");
    }

    /**
     * Create a new instance of the keystore provider.
     *
     * @param workingDirectory The working directory for the server.
     * @param keystorePass     A password for the keystore.
     * @param certificatePass  A password for the cert private key.
     * @param alias            A certificate alias.
     */
    public GeneratedKeystoreProvider(final String workingDirectory,
                                     final String keystorePass,
                                     final String certificatePass,
                                     final String alias) {
        keystorePath = Paths.get(workingDirectory, "generated.p12")
                .toAbsolutePath().toString();

        this.generatedProvider = new FSKeystoreProvider(
                keystorePath, keystorePass, KEYSTORE_TYPE);

        this.keystorePass = keystorePass;
        this.certificatePass = certificatePass;
        this.alias = alias;
    }

    /**
     * Check to see if the generated keystore exists.
     *
     * @return True if the file exists. Makes no assumption about passwords.
     */
    private boolean doesKeystoreExist() {
        File ks = new File(keystorePath);
        return ks.exists();
    }

    /**
     * Get the certificate builder for this generator.
     *
     * @param keyPair The keypair to use for generating certificates.
     * @return A certificate builder, using the public key.
     * @throws NoSuchAlgorithmException Thrown if the RSA keygen alg is not
     *                                  available.
     */
    protected X509v3CertificateBuilder getCertificateBuilder(
            final KeyPair keyPair)
            throws NoSuchAlgorithmException {
        logger.info("Generating Certificate Builder");
        byte[] publicKey = keyPair.getPublic().getEncoded();
        SubjectPublicKeyInfo pki =
                SubjectPublicKeyInfo.getInstance(publicKey);
        return new X509v3CertificateBuilder(
                X_500_NAME, SERIAL, NOT_BEFORE, NOT_AFTER, X_500_NAME, pki);
    }

    /**
     * Get the certificate builder for this generator.
     *
     * @param keyPair The keypair to use for signing.
     * @return A certificate builder, using the public key.
     * @throws NoSuchAlgorithmException  Thrown if the RSA keygen alg is not
     *                                   available.
     * @throws IOException               Thrown if the generated key cannot be
     *                                   read.
     * @throws OperatorCreationException Thrown if we cannot create a content
     *                                   signer.
     */
    protected ContentSigner getCertificateSigner(final KeyPair keyPair)
            throws NoSuchAlgorithmException,
            IOException, OperatorCreationException {
        logger.info("Generating Certificate Signer");

        // Create the certificate signer.
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        AlgorithmIdentifier sigAlgId =
                new DefaultSignatureAlgorithmIdentifierFinder()
                        .find("SHA256WithRSAEncryption");
        AlgorithmIdentifier digAlgId =
                new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        return new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(PrivateKeyFactory.createKey(privateKey));
    }

    /**
     * Retrieve the keystore from this provider.
     *
     * @return The private key.
     */
    public KeyStore getKeyStore() {
        if (!doesKeystoreExist()) {
            generateKeystore();
        }
        return generatedProvider.getKeyStore();
    }

    /**
     * Generate a new keystore at the provided file path.
     */
    protected void generateKeystore() {
        try {
            // Build a new keystore.
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, this.keystorePass.toCharArray());

            logger.info("Generating Keypair");
            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024 * 2);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            Key key = keyPair.getPrivate();

            logger.info("Generating x509 Certificate");
            ContentSigner signer = getCertificateSigner(keyPair);
            X509v3CertificateBuilder builder = getCertificateBuilder(keyPair);
            X509CertificateHolder holder = builder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter()
                    .getCertificate(holder);
            Certificate[] chain = new Certificate[]{cert};

            // Add the certificate and the key
            keyStore.setKeyEntry(alias,
                    key,
                    this.certificatePass.toCharArray(),
                    chain);

            // Write it to the filesystem
            File targetFile = new File(keystorePath);
            FileOutputStream os = new FileOutputStream(targetFile);
            keyStore.store(os, this.keystorePass.toCharArray());
        } catch (Exception kse) {
            throw new RuntimeException(kse);
        }
    }

    /**
     * Store the keystore to a provided output stream.
     *
     * @param outputStream The output stream to store to.
     */
    @Override
    public void writeTo(final OutputStream outputStream) {
        getKeyStore(); // Make sure the keystore exists.
        generatedProvider.writeTo(outputStream);
    }
}
