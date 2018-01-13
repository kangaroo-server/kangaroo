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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for the forbidden exception.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2ForbiddenExceptionTest {

    /**
     * Assert that we can create an exception, and that the response values
     * are as expected.
     */
    @Test
    public void assertConstructor() {
        UriInfo mockInfo = Mockito.mock(UriInfo.class);
        List<String> mockPaths = Arrays.asList("foo");

        UriBuilder mockBuilder = UriBuilder.fromUri("http://localhost/");

        Mockito.doReturn(mockBuilder).when(mockInfo).getBaseUriBuilder();
        Mockito.doReturn(mockPaths).when(mockInfo).getMatchedURIs();

        String[] requiredScopes = new String[]{"one", "two"};

        OAuth2ForbiddenException e =
                new OAuth2ForbiddenException(mockInfo, requiredScopes);

        Assert.assertEquals(OAuth2ForbiddenException.CODE, e.getCode());
        Assert.assertEquals("forbidden", e.getMessage());
    }
}
