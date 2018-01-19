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

package net.krotscheck.kangaroo.authz.admin.v1.auth;

import net.krotscheck.kangaroo.authz.admin.v1.auth.exception.WWWChallengeExceptionMapper;
import org.junit.Test;

import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for the feature injector.
 *
 * @author Michael Krotscheck
 */
public final class OAuth2AuthFeatureTest {

    /**
     * Assert that we are injecting expected features.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {

        OAuth2AuthFeature feature = new OAuth2AuthFeature();
        FeatureContext mockContext = mock(FeatureContext.class);

        boolean result = feature.configure(mockContext);
        assertTrue(result);

        verify(mockContext, times(1))
                .register(OAuth2ScopeDynamicFeature.class);
        verify(mockContext, times(1))
                .register(any(WWWChallengeExceptionMapper.Binder.class));

        verifyNoMoreInteractions(mockContext);
    }
}
