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

package net.krotscheck.kangaroo.authz.oauth2.authn.authz;

import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2Principal;
import net.krotscheck.kangaroo.authz.oauth2.authn.O2SecurityContext;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.AccessDeniedException;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import java.security.Principal;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the authorization filter. Mostly decoupled from the actual
 * flow here, as it's easier to control.
 *
 * @author Michael Krotscheck
 */
public final class O2AuthorizationFilterTest {

    /**
     * Mock context.
     */
    private ContainerRequestContext requestContext;

    /**
     * Mock security context.
     */
    private O2SecurityContext securityContext;

    /**
     * Setup the test.
     */
    @Before
    public void setup() {
        requestContext = mock(ContainerRequestContext.class);
        securityContext = mock(O2SecurityContext.class);

        doReturn(securityContext)
                .when(requestContext)
                .getSecurityContext();
    }

    /**
     * If the wrong principal type is provided, throw.
     */
    @Test(expected = AccessDeniedException.class)
    public void assertFailWithWrongPrincipalType() {
        O2AuthorizationFilter filter = new O2AuthorizationFilter(true, true);

        Principal principal = () -> "wrong type";
        doReturn(principal)
                .when(securityContext)
                .getUserPrincipal();

        filter.filter(requestContext);
    }

    /**
     * If no principal is provided, throw.
     */
    @Test(expected = AccessDeniedException.class)
    public void assertFailWithNoPrincipal() {
        O2AuthorizationFilter filter = new O2AuthorizationFilter(true, true);

        doReturn(null)
                .when(securityContext)
                .getUserPrincipal();

        filter.filter(requestContext);
    }

    /**
     * If the principal has no client, throw.
     */
    @Test(expected = AccessDeniedException.class)
    public void assertFailWithNoClient() {
        O2AuthorizationFilter filter = new O2AuthorizationFilter(true, true);

        O2Principal p = new O2Principal();

        doReturn(p)
                .when(securityContext)
                .getUserPrincipal();

        filter.filter(requestContext);
    }

    /**
     * If we have a public client, pass.
     */
    @Test
    public void assertPassWithPublicClient() {
        O2AuthorizationFilter filter = new O2AuthorizationFilter(true, true);

        Client c = new Client();
        c.setId(IdUtil.next());
        O2Principal p = new O2Principal(c);

        doReturn(p)
                .when(securityContext)
                .getUserPrincipal();

        filter.filter(requestContext);
    }

    /**
     * If we have a public client but it's not permitted, throw.
     */
    @Test(expected = AccessDeniedException.class)
    public void assertFailWithNonpermittedPublicClient() {
        O2AuthorizationFilter filter = new O2AuthorizationFilter(true, false);

        Client c = new Client();
        c.setId(IdUtil.next());
        O2Principal p = new O2Principal(c);

        doReturn(p)
                .when(securityContext)
                .getUserPrincipal();

        filter.filter(requestContext);
    }

    /**
     * If we have a private client but it's not permitted, throw.
     */
    @Test(expected = AccessDeniedException.class)
    public void assertFailWithNonpermittedPrivateClient() {
        O2AuthorizationFilter filter = new O2AuthorizationFilter(false, true);

        Client c = new Client();
        c.setId(IdUtil.next());
        c.setClientSecret("private_secret");
        O2Principal p = new O2Principal(c);

        doReturn(p)
                .when(securityContext)
                .getUserPrincipal();

        filter.filter(requestContext);
    }
}
