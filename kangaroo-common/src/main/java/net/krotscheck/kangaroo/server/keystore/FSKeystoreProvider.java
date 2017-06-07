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

import java.io.FileInputStream;
import java.io.OutputStream;
import java.security.KeyStore;

/**
 * Read an keystore certificate and keypair from an existing PCKS on the
 * filesystem.
 *
 * @author Michael Krotscheck
 */
public final class FSKeystoreProvider implements IKeystoreProvider {

    /**
     * The loaded keystore.
     */
    private KeyStore keyStore;

    /**
     * The keystore path.
     */
    private String keystorePath;

    /**
     * The keystore's password.
     */
    private String keystorePass;

    /**
     * The type of keystore.
     */
    private String keystoreType;

    /**
     * Create a new filesystem keystore provider, with the provided path and
     * passwords.
     *
     * @param keystorePath Filesystem path to the keystore.
     * @param keystorePass Password for the keystore.
     * @param keystoreType The keystore type.
     */
    public FSKeystoreProvider(final String keystorePath,
                              final String keystorePass,
                              final String keystoreType) {
        this.keystorePath = keystorePath;
        this.keystorePass = keystorePass;
        this.keystoreType = keystoreType;
    }

    /**
     * Retrieve the keystore from this provider.
     *
     * @return The private key.
     */
    public KeyStore getKeyStore() {
        if (keyStore == null) {
            try {
                // Load the keystore.
                keyStore = KeyStore.getInstance(keystoreType);
                FileInputStream fis = new FileInputStream(keystorePath);
                keyStore.load(fis, keystorePass.toCharArray());
            } catch (Exception e) {
                throw new RuntimeException(e);
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
