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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import net.krotscheck.kangaroo.common.cors.ICORSValidator;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This validator returns true if the CORS domain exists in our referrer
 * table. Results are cached for 5 minutes, which is our CORS cache timeout.
 *
 * @author Michael Krotscheck
 */
public final class HibernateCORSValidator implements ICORSValidator {

    /**
     * This cache contains the last 1000 domains that were checked.
     */
    private final LoadingCache<URI, Boolean> validDomains;

    /**
     * The hibernate CORS session factory.
     *
     * @param cacheLoader The CORS cache loader.
     */
    @Inject
    public HibernateCORSValidator(final HibernateCORSCacheLoader cacheLoader) {
        this.validDomains = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(cacheLoader);
    }

    /**
     * Return true if the URI is a valid origin for a CORS request.
     *
     * @param origin The origin to validate. Will only contain scheme, host,
     *               and port.
     * @return True if the origin is valid, otherwise false.
     */
    @Override
    public boolean isValidCORSOrigin(final URI origin) {
        try {
            return validDomains.get(origin);
        } catch (ExecutionException ee) {
            return false;
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(HibernateCORSValidator.class)
                    .to(ICORSValidator.class)
                    .in(Singleton.class);
        }
    }
}
