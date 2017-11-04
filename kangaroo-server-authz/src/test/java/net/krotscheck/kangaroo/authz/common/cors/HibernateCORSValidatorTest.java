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

package net.krotscheck.kangaroo.authz.common.cors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * Unit tests for the ICORSValidator.
 *
 * @author Michael Krotscheck
 */
public class HibernateCORSValidatorTest {

    /**
     * Assert that we can load valid domains.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void loadValid() throws Exception {
        HibernateCORSCacheLoader loader =
                Mockito.mock(HibernateCORSCacheLoader.class);
        Mockito.doReturn(true)
                .when(loader)
                .load(Matchers.any(URI.class));

        HibernateCORSValidator validator = new HibernateCORSValidator(loader);

        URI uri = new URI("http://valid.example.com");
        Boolean response1 = validator.isValidCORSOrigin(uri);
        validator.isValidCORSOrigin(uri);

        Assert.assertTrue(response1);

        Mockito.verify(loader, Mockito.times(1))
                .load(Matchers.any(URI.class));
    }

    /**
     * Assert that we still return valid data in the case an exception is
     * thrown.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void loadValidWithException() throws Exception {
        HibernateCORSCacheLoader loader =
                Mockito.mock(HibernateCORSCacheLoader.class);
        Mockito.doThrow(ExecutionException.class)
                .when(loader)
                .load(Matchers.any(URI.class));

        HibernateCORSValidator validator = new HibernateCORSValidator(loader);

        URI uri = new URI("http://valid.example.com");
        Boolean response1 = validator.isValidCORSOrigin(uri);
        validator.isValidCORSOrigin(uri);

        Assert.assertFalse(response1);

        Mockito.verify(loader, Mockito.times(2))
                .load(Matchers.any(URI.class));
    }
}
