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

package net.krotscheck.kangaroo.server;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

/**
 * List of configuration settings, and their defaults.
 *
 * @author Michael Krotscheck
 */
public final class Config {

    /**
     * Configuration key, and default, for the IP address to which this
     * server will bind itself.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final Entry<String, String> HOST = new
            SimpleImmutableEntry<>("kangaroo.host", "127.0.0.1");

    /**
     * Configuration key, and default, for the port on which this server will
     * listen.
     */
    public static final Entry<String, Integer> PORT = new
            SimpleImmutableEntry<>("kangaroo.port", 8080);

    /**
     * The filesystem path used for the server's working directory. Used
     * throughout the application.
     */
    public static final Entry<String, String> WORKING_DIR = new
            SimpleImmutableEntry<>("kangaroo.working_dir", "/var/lib/kangaroo");

    /**
     * Configuration property for an externally provided keystore.
     */
    public static final Entry<String, String> KEYSTORE_PATH = new
            SimpleImmutableEntry<>("kangaroo.keystore_path", null);

    /**
     * Configuration property for the password required by the keystore.
     */
    public static final Entry<String, String> KEYSTORE_PASS = new
            SimpleImmutableEntry<>("kangaroo.keystore_password", "kangaroo");

    /**
     * Configuration property for the keystore type provided.
     */
    public static final Entry<String, String> KEYSTORE_TYPE = new
            SimpleImmutableEntry<>("kangaroo.keystore_type", "PKCS12");

    /**
     * The alias of the cert to use from the keystore.
     */
    public static final Entry<String, String> CERT_ALIAS = new
            SimpleImmutableEntry<>("kangaroo.cert_alias", "kangaroo");

    /**
     * The password for cert's private key.
     */
    public static final Entry<String, String> CERT_KEY_PASS = new
            SimpleImmutableEntry<>("kangaroo.cert_key_password", "kangaroo");

    /**
     * Filesystem path to the directory to use as our HTML5 Application root.
     */
    public static final Entry<String, String> HTML_APP_ROOT = new
            SimpleImmutableEntry<>("kangaroo.html_app_root", null);

    /**
     * Private constructor for a utility class.
     */
    private Config() {

    }
}
