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

package net.krotscheck.kangaroo.common.security;

import com.google.common.collect.Sets;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for our custom protection filter.
 *
 * @author Michael Krotscheck
 */
public final class CsrfProtectionFilterTest {

    /**
     * Mock request.
     */
    private ContainerRequestContext mockContext;

    /**
     * Setup the test.
     */
    @Before
    public void setup() {
        mockContext = mock(ContainerRequestContext.class);
    }

    /**
     * Assert that GET/OPTIONS/HEAD are ignored.
     */
    @Test
    public void assertIgnoresBasicMethods() {
        CsrfProtectionFilter filter = new CsrfProtectionFilter();

        Sets.newHashSet("GET", "OPTIONS", "HEAD")
                .forEach((method) -> {
                    doReturn(method).when(mockContext).getMethod();
                    filter.filter(mockContext);
                });
    }

    /**
     * Assert that the filter will fail the request if no header is included.
     */
    @Test(expected = BadRequestException.class)
    public void assertFailsWithoutHeader() {
        CsrfProtectionFilter filter = new CsrfProtectionFilter();
        doReturn("POST").when(mockContext).getMethod();

        MultivaluedStringMap map = new MultivaluedStringMap();
        doReturn(map).when(mockContext).getHeaders();
        filter.filter(mockContext);
    }

    /**
     * Ensure that the header passes.
     */
    @Test
    public void assertPassesWithHeader() {
        CsrfProtectionFilter filter = new CsrfProtectionFilter();
        doReturn("POST").when(mockContext).getMethod();

        MultivaluedStringMap map = new MultivaluedStringMap();
        map.putSingle("X-Requested-With", "Test");
        doReturn(map).when(mockContext).getHeaders();
        filter.filter(mockContext);
    }
}
