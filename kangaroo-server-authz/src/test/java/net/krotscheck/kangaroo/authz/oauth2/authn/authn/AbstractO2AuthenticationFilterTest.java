/*
 * Copyright (c) 2018 Michael Krotscheck
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

package net.krotscheck.kangaroo.authz.oauth2.authn.authn;

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2SecurityContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.glassfish.jersey.server.ContainerRequest;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.inject.Provider;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit test for common authn filter methods.
 *
 * @author Michael Krotscheck
 */
public final class AbstractO2AuthenticationFilterTest {

    /**
     * The filter under test.
     */
    private AbstractO2AuthenticationFilter testFilter;

    /**
     * The mock request.
     */
    private ContainerRequest mockContainerRequest;

    /**
     * A mock hibernate session.
     */
    private Session mockSession;

    /**
     * A mock security context.
     */
    private SecurityContext mockSecurityContext;

    /**
     * Setup our test.
     */
    @Before
    public void setup() {
        mockContainerRequest = mock(ContainerRequest.class);
        mockSession = mock(Session.class);
        mockSecurityContext = mock(SecurityContext.class);

        doReturn(mockSecurityContext)
                .when(mockContainerRequest)
                .getSecurityContext();

        Provider<ContainerRequest> requestProvider = () -> mockContainerRequest;
        Provider<Session> sessionProvider = () -> mockSession;

        testFilter = new AbstractO2AuthenticationFilter(
                requestProvider, sessionProvider) {
            @Override
            public void filter(final ContainerRequestContext requestContext) {
                // Do nothing.
            }
        };
    }

    /**
     * Test passthrough getters.
     */
    @Test
    public void getSession() {
        assertSame(mockSession, testFilter.getSession());
        assertSame(mockContainerRequest, testFilter.getRequest());
        assertSame(mockSecurityContext, testFilter.getSecurityContext());
    }

    /**
     * Setting the principal should merge with any existing, and set the new
     * SecurityContext.
     */
    @Test
    public void setPrincipal() {
        ArgumentCaptor<SecurityContext> scCaptor =
                ArgumentCaptor.forClass(SecurityContext.class);

        mockContainerRequest = spy(mockContainerRequest);

        Client c = new Client();
        c.setId(IdUtil.next());
        c.setClientSecret("secret");
        O2Principal p = new O2Principal(c);

        assertFalse(testFilter.getSecurityContext()
                instanceof O2SecurityContext);
        testFilter.setPrincipal(p);
        verify(mockContainerRequest).setSecurityContext(scCaptor.capture());

        SecurityContext setContext = scCaptor.getValue();
        assertNotNull(setContext);
        assertTrue(setContext instanceof O2SecurityContext);
        O2Principal setPrincipal =
                (O2Principal) setContext.getUserPrincipal();
        assertEquals(p, setPrincipal);
    }

    /**
     * Convert Credentials should never throw any exceptions.
     */
    @Test
    public void convertCredentials() {
        BigInteger validId = IdUtil.next();
        String validRawId = IdUtil.toString(validId);

        assertNull(testFilter
                .convertCredentials(null, "secret"));
        assertNull(testFilter
                .convertCredentials("malformed", "secret"));
        assertEquals(new SimpleEntry<>(validId, "secret"),
                testFilter.convertCredentials(validRawId, "secret"));
        assertEquals(new SimpleEntry<>(validId, null),
                testFilter.convertCredentials(validRawId, null));
        assertEquals(new SimpleEntry<>(validId, null),
                testFilter.convertCredentials(validRawId, ""));
    }
}
