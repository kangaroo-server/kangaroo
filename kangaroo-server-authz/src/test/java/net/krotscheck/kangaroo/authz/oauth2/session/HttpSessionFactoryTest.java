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

package net.krotscheck.kangaroo.authz.oauth2.session;

import org.junit.Test;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the session factory.
 *
 * @author Michael Krotscheck
 */
public final class HttpSessionFactoryTest {

    /**
     * Assert that it returns the session instance provided by the HTTP
     * Servlet request.
     */
    @Test
    public void get() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpSession mockSession = mock(HttpSession.class);
        doReturn(mockSession).when(mockRequest).getSession();
        Provider<HttpServletRequest> requestProvider = () -> mockRequest;

        HttpSessionFactory factory = new HttpSessionFactory(requestProvider);
        HttpSession responseSession = factory.get();
        assertSame(responseSession, mockSession);
    }
}