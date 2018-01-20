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

package net.krotscheck.kangaroo.authz.admin.v1.auth.exception;

import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


/**
 * Unit tests for exceptions that carry an authorization challenge.
 *
 * @author Michael Krotscheck
 */
public final class WWWChallengeExceptionTest {

    /**
     * Assert that we can create an exception, and expect the provided
     * variables in response.
     */
    @Test
    public void assertConstructorWithPath() {
        UriInfo mockInfo = Mockito.mock(UriInfo.class);
        List<String> mockPaths = Arrays.asList("foo");

        UriBuilder mockBuilder = UriBuilder.fromUri("http://localhost/");

        Mockito.doReturn(mockBuilder).when(mockInfo).getBaseUriBuilder();
        Mockito.doReturn(mockPaths).when(mockInfo).getMatchedURIs();

        String[] requiredScopes = new String[]{"one", "two"};

        TestException e = new TestException(mockInfo, requiredScopes);

        assertSame(requiredScopes, e.getRequiredScopes());
        assertEquals("http://localhost/foo", e.getRealm().toString());
    }

    /**
     * Assert that constructing the realm will drop all but the first string.
     */
    @Test
    public void assertConstructorWithManyPaths() {
        UriInfo mockInfo = Mockito.mock(UriInfo.class);
        List<String> mockPaths = Arrays.asList("one", "two", "three");

        UriBuilder mockBuilder = UriBuilder.fromUri("http://localhost/");

        Mockito.doReturn(mockBuilder).when(mockInfo).getBaseUriBuilder();
        Mockito.doReturn(mockPaths).when(mockInfo).getMatchedURIs();

        String[] requiredScopes = new String[]{"one", "two"};

        TestException e = new TestException(mockInfo, requiredScopes);

        assertSame(requiredScopes, e.getRequiredScopes());
        assertEquals("http://localhost/three", e.getRealm().toString());
    }

    /**
     * Assert that constructing the realm will drop all but the first string.
     */
    @Test
    public void assertConstructorWithNoPaths() {
        UriInfo mockInfo = Mockito.mock(UriInfo.class);
        List<String> mockPaths = new ArrayList<>();

        UriBuilder mockBuilder = UriBuilder.fromUri("http://localhost/");

        Mockito.doReturn(mockBuilder).when(mockInfo).getBaseUriBuilder();
        Mockito.doReturn(mockPaths).when(mockInfo).getMatchedURIs();

        String[] requiredScopes = new String[]{"one", "two"};

        TestException e = new TestException(mockInfo, requiredScopes);

        assertSame(requiredScopes, e.getRequiredScopes());
        assertEquals("http://localhost/", e.getRealm().toString());

    }

    /**
     * Test exception.
     */
    public static final class TestException extends WWWChallengeException {

        /**
         * The error code.
         */
        public static final ErrorCode CODE = new ErrorCode(
                Status.UNAUTHORIZED,
                "test_error",
                "Test Error"
        );


        /**
         * Create a new exception with the specified error code.
         *
         * @param requestInfo    The original URI request, from which we're
         *                       going to derive our realm.
         * @param requiredScopes A list of required scopes.
         */
        protected TestException(final UriInfo requestInfo,
                                final String[] requiredScopes) {
            super(CODE, requestInfo, requiredScopes);
        }
    }
}
