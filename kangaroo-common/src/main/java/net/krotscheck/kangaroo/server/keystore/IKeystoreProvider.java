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

import java.io.OutputStream;
import java.security.KeyStore;

/**
 * Interface describing a keystore provider. These classes all perform
 * essentially the same thing: Read a key and certificate chain from
 * a configurable source, then present it as a Keystore instance.
 * <p>
 * This permits us to choose a single certificate among many, and/or generate
 * our own if needed.
 *
 * @author Michael Krotscheck
 */
public interface IKeystoreProvider {

    /**
     * Return the configured keystore.
     *
     * @return The keystore which this provider owns.
     */
    KeyStore getKeyStore();

    /**
     * Store the keystore to a provided output stream.
     *
     * @param outputStream The output stream to store to.
     */
    void writeTo(OutputStream outputStream);
}
