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

package net.krotscheck.kangaroo.authz.admin.v1.auth.filter;

import net.krotscheck.kangaroo.authz.admin.v1.auth.exception.OAuth2ForbiddenException;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit test for the authorization step of the OAuth2 Dynamic filter chain.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2AuthorizationFilterTest {

    /**
     * Request context.
     */
    private ContainerRequestContext requestContext;

    /**
     * Security context.
     */
    private SecurityContext securityContext;

    /**
     * Setup of common mocks.
     */
    @Before
    public void setup() {
        requestContext = mock(ContainerRequestContext.class);
        securityContext = mock(SecurityContext.class);
        UriInfo mockInfo = mock(UriInfo.class);
        UriBuilder mockBuilder = UriBuilder.fromPath("http://example.com/");
        List<String> matchedPaths = Collections.singletonList("path");

        doReturn(securityContext).when(requestContext).getSecurityContext();
        doReturn(false).when(securityContext).isUserInRole("invalid");
        doReturn(true).when(securityContext).isUserInRole("valid");
        doReturn(mockInfo).when(requestContext).getUriInfo();
        doReturn(mockBuilder).when(mockInfo).getBaseUriBuilder();
        doReturn(matchedPaths).when(mockInfo).getMatchedURIs();
    }

    /**
     * Assert that this filter has the Authorization priority.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertCorrectPriority() throws Exception {
        Priority a = OAuth2AuthorizationFilter.class
                .getAnnotation(Priority.class);
        assertEquals(Priorities.AUTHORIZATION, a.value());
    }

    /**
     * Assert that a valid set of scopes resolves.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void assertValidScopes() throws Exception {
        OAuth2AuthorizationFilter filter =
                new OAuth2AuthorizationFilter(new String[]{"valid"});
        filter.filter(requestContext);
    }

    /**
     * Assert that a resource with no annotated scopes throws.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2ForbiddenException.class)
    public void assertResourceWithNoScopes() throws Exception {
        OAuth2AuthorizationFilter filter = new OAuth2AuthorizationFilter();
        filter.filter(requestContext);
    }

    /**
     * If we, for some reason, get a resource with no security context, throw.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2ForbiddenException.class)
    public void assertNoSecurityContext() throws Exception {
        doReturn(null).when(requestContext).getSecurityContext();

        OAuth2AuthorizationFilter filter = new OAuth2AuthorizationFilter();
        filter.filter(requestContext);
    }

    /**
     * Assert that a scoped token that doesn't match fails.
     *
     * @throws Exception Should be thrown.
     */
    @Test(expected = OAuth2ForbiddenException.class)
    public void assertWithMismatchedScopes() throws Exception {
        OAuth2AuthorizationFilter filter =
                new OAuth2AuthorizationFilter(new String[]{"invalid"});
        filter.filter(requestContext);
    }
}
