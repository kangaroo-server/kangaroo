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

package net.krotscheck.kangaroo.common.cors;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Singleton;
import java.net.URI;

/**
 * Test CORS validator for unit tests.
 *
 * @author Michael Krotscheck
 */
public final class TestCORSValidator implements ICORSValidator {

    /**
     * Return true if the origin is 'http://valid.example.com'.
     *
     * @param origin A test origin.
     * @return True or false, depending
     */
    @Override
    public boolean isValidCORSOrigin(final URI origin) {
        return origin.toString().equals("http://valid.example.com");
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(TestCORSValidator.class)
                    .to(ICORSValidator.class)
                    .in(Singleton.class);
        }
    }
}
