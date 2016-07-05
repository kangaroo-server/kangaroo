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

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.test.TestAuthenticator.Binder;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Smoke test for our dummy authenticator.
 *
 * @author Michael Krotscheck
 */
public final class TestAuthenticatorTest extends DatabaseTest {

    /**
     * Test context.
     */
    private EnvironmentBuilder context;

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        context = new EnvironmentBuilder(getSession())
                .client(ClientType.AuthorizationGrant)
                .authenticator("test")
                .user();

        List<IFixture> fixtures = new ArrayList<>();
        fixtures.add(context);
        return fixtures;
    }

    /**
     * Load the test data.
     *
     * @return The test data.
     */
    @Override
    public File testData() {
        return null;
    }

    /**
     * Assert that calling the delegate method immediately redirects to the
     * redirect URL.
     *
     * @throws Exception Thrown when an unexpected error is encountered.
     */
    @Test
    public void testDelegate() throws Exception {
        IAuthenticator a = new TestAuthenticator(getSession());
        URI callbackUri = UriBuilder.fromUri("http://example.com").build();
        Response r = a.delegate(context.getAuthenticator(), callbackUri);

        Assert.assertEquals(callbackUri, r.getLocation());
    }

    /**
     * Assert that calling authenticate creates a new user if necessary.
     *
     * @throws Exception Thrown when an unexpected error is encountered.
     */
    @Test
    public void testAuthenticate() throws Exception {
        IAuthenticator a = new TestAuthenticator(getSession());
        UserIdentity i =
                a.authenticate(context.getAuthenticator(),
                        new MultivaluedHashMap<>());
        Assert.assertEquals(TestAuthenticator.REMOTE_ID,
                i.getRemoteId());

        // Authenticate again, this time triggering the not-new-user flow.
        UserIdentity i2 =
                a.authenticate(context.getAuthenticator(),
                        new MultivaluedHashMap<>());
        Assert.assertSame(i, i2);
    }

    /**
     * Assert that we can invoke the binder.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator locator = factory.create("TestAuthenticatorTest");

        Binder b = new TestAuthenticator.Binder();
        ServiceLocatorUtilities.bind(locator, b);

        List<ActiveDescriptor<?>> descriptors =
                locator.getDescriptors(
                        BuilderHelper.createNameAndContractFilter(
                                IAuthenticator.class.getName(), "test"
                        ));
        Assert.assertEquals(1, descriptors.size());

        ActiveDescriptor descriptor = descriptors.get(0);
        Assert.assertNotNull(descriptor);
        // Check scope...
        Assert.assertEquals(RequestScoped.class.getCanonicalName(),
                descriptor.getScope());

        // ... check name.
        Assert.assertEquals("test", descriptor.getName());
    }
}
