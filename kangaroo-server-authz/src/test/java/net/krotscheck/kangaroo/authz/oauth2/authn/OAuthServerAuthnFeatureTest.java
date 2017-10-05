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

package net.krotscheck.kangaroo.authz.oauth2.authn;

import net.krotscheck.kangaroo.authz.oauth2.authn.factory.CredentialsFactory;
import net.krotscheck.kangaroo.authz.oauth2.authn.filter.ClientAuthorizationFilter;
import org.junit.Test;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Assert that binding the Authn feature includes expected components.
 *
 * @author Michael Krotscheck
 */
public class OAuthServerAuthnFeatureTest {

    /**
     * Quick check to see if we can inject and access the configuration.
     */
    @Test
    public void testInjection() {
        Feature f = new OAuthServerAuthnFeature();
        FeatureContext context = mock(FeatureContext.class);

        f.configure(context);

        verify(context, atLeastOnce())
                .register(any(CredentialsFactory.Binder.class));
        verify(context, atLeastOnce())
                .register(any(ClientAuthorizationFilter.Binder.class));

        verifyNoMoreInteractions(context);
    }
}
