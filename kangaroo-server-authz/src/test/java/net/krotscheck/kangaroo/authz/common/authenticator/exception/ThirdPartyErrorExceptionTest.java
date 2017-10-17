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

package net.krotscheck.kangaroo.authz.common.authenticator.exception;

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for our error exception.
 *
 * @author Michael Krotscheck
 */
public final class ThirdPartyErrorExceptionTest {

    /**
     * Test the default constructor.
     */
    @Test
    public void testDefaultConstructor() {
        ThirdPartyErrorException e = new ThirdPartyErrorException();
        assertEquals(Status.SERVICE_UNAVAILABLE,
                e.getCode().getHttpStatus());
        assertEquals("service_unavailable",
                e.getCode().getError());
        assertEquals("Unexpected error from an external service.",
                e.getCode().getErrorDescription());
    }

    /**
     * Test a variety of constructors.
     */
    @Test
    public void testErrorMultivaluedParameterConstructor() {
        MultivaluedMap<String, String> params = new MultivaluedStringMap();
        params.add("error", "foo");
        params.add("error_description", "bar");

        ThirdPartyErrorException e = new ThirdPartyErrorException(params);
        assertEquals(Status.SERVICE_UNAVAILABLE,
                e.getCode().getHttpStatus());
        assertEquals("foo",
                e.getCode().getError());
        assertEquals("bar",
                e.getCode().getErrorDescription());
    }

    /**
     * Test a variety of constructors.
     */
    @Test
    public void testErrorMapParameterConstructor() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("error", "foo");
        params.put("error_description", "bar");

        ThirdPartyErrorException e = new ThirdPartyErrorException(params);
        assertEquals(Status.SERVICE_UNAVAILABLE,
                e.getCode().getHttpStatus());
        assertEquals("foo",
                e.getCode().getError());
        assertEquals("bar",
                e.getCode().getErrorDescription());
    }
}
