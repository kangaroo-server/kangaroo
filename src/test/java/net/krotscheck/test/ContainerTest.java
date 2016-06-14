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

package net.krotscheck.test;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.hibernate.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * A test suite sets up an entire application container to run our tests
 * against.
 *
 * @author Michael Krotscheck
 */
public abstract class ContainerTest extends JerseyTest {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory.getLogger(ContainerTest.class);

    /**
     * The database management instance.
     */
    private static DatabaseManager manager = new DatabaseManager();

    /**
     * Configure all jersey2 clients.
     *
     * @param config The configuration instance to modify.
     */
    @Override
    protected final void configureClient(final ClientConfig config) {
        config.property(ClientProperties.FOLLOW_REDIRECTS, false);
    }

    /**
     * Setup a database for our application.
     *
     * @throws Exception Initialization exception.
     */
    @BeforeClass
    public static void setupDatabaseSchema() throws Exception {
        manager.setupJNDI();
        manager.setupDatabase();
    }

    /**
     * Shut down the database.
     *
     * @throws Exception Teardown Exceptions.
     */
    @AfterClass
    public static void removeDatabaseSchema() throws Exception {
        manager.clearTestData();
        manager.cleanDatabase();
    }

    /**
     * Cleanup the session factory after every run.
     */
    @After
    public final void clearSession() {
        manager.cleanSessions();
    }

    /**
     * Create and return a hibernate session for the test database.
     *
     * @return The constructed session.
     */
    protected final Session getSession() {
        return manager.getSession();
    }

    /**
     * Load some test data into our database.
     *
     * @param testData The test data xml file to map.
     */
    public static void loadTestData(final File testData) {
        manager.loadTestData(testData);
    }

    /**
     * Helper method, which extracts the query string from a URI.
     *
     * @param uri The URI from which we're pulling the query response.
     * @return A map of all responses.
     */
    protected final Map<String, String> parseQueryParams(final URI uri) {
        return parseQueryParams(uri.getRawQuery());
    }

    /**
     * Helper method, which extracts parameters from a query string.
     *
     * @param query The Query string to decode.
     * @return A map of all responses.
     */
    protected final Map<String, String> parseQueryParams(final String query) {
        List<NameValuePair> params = URLEncodedUtils
                .parse(query, Charset.forName("ISO-8859-1"));

        Map<String, String> map = new HashMap<>();
        for (NameValuePair pair : params) {
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    /**
     * Helper method, which extracts the query string from a response body.
     *
     * @param response The HTTP query response to extract the body from.
     * @return A map of all variables in the inputstream.
     */
    protected final Map<String, String> parseBodyParams(final Response
                                                                response) {
        try {
            Charset charset = Charset.forName("UTF-8");
            InputStream stream = (InputStream) response.getEntity();
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, charset);
            return parseQueryParams(writer.toString());
        } catch (IOException ioe) {
            logger.error("Could not decode params in stream.", ioe);
        }
        return new HashMap<>();
    }

    /**
     * Build an authorization header.
     *
     * @param user     The user.
     * @param password The password.
     * @return "Basic [base64(user+":"+password)]
     */
    protected final String buildAuthorizationHeader(final UUID user,
                                                    final String password) {
        StringBuilder credBuilder = new StringBuilder();
        credBuilder.append(user.toString()).append(":").append(password);
        byte[] bytesEncoded =
                Base64.encodeBase64(credBuilder.toString().getBytes());

        StringBuilder authBuilder = new StringBuilder();
        authBuilder.append("Basic ").append(new String(bytesEncoded));
        return authBuilder.toString();
    }

    /**
     * This is a convenience method which, presuming a response constitutes a
     * redirect, will follow that redirect and return the subsequent response.
     *
     * @param original The original response, which should constitute a
     *                 redirect.
     * @return The response.
     */
    protected final Response followRedirect(final Response original) {

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, original.getStatus());

        URI uri = original.getLocation();
        WebTarget target = target().path(uri.getPath());

        // Iterate through the query parameters and apply them.
        for (NameValuePair pair : URLEncodedUtils.parse(uri, "UTF-8")) {
            target = target.queryParam(pair.getName(), pair.getValue());
        }

        return target.request().get();
    }
}
