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

package net.krotscheck.kangaroo.authz.oauth2.exception;

import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidScopeException;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * Test for the redirecting exception.
 *
 * @author Michael Krotscheck
 */
public final class RedirectingExceptionTest {

    /**
     * Test that the constructor is reasonably functional.
     */
    @Test
    public void testConstructor() {
        KangarooException cause = new InvalidScopeException();
        URI redirect = UriBuilder.fromUri("http://redirect.example.com")
                .build();
        RedirectingException r = new RedirectingException(cause, redirect,
                ClientType.Implicit);

        assertEquals(ClientType.Implicit, r.getClientType());
        assertEquals(redirect, r.getRedirect());
        assertEquals(cause.getMessage(), r.getMessage());
        assertEquals(cause, r.getCause());
    }
}
