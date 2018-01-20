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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import net.krotscheck.kangaroo.util.StringUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for the introspection response.
 *
 * @author Michael Krotscheck
 */
public final class IntrospectionResponseEntityTest {

    /**
     * Create a testing token.
     *
     * @return A token with basic defaults set, w/o Client/Flow specific values.
     */
    private OAuthToken buildToken() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);

        Application a = new Application();
        a.setId(IdUtil.next());

        Client c = new Client();
        c.setId(IdUtil.next());
        c.setApplication(a);

        OAuthToken token = new OAuthToken();
        token.setId(IdUtil.next());
        token.setCreatedDate(now);
        token.setModifiedDate(now);
        token.setExpiresIn(600);
        token.setTokenType(OAuthTokenType.Bearer);
        token.setClient(c);
        token.setIssuer("localhost");

        return token;
    }

    /**
     * Create a testing identity and user.
     *
     * @return A simple user identity with remote ID and user record.
     */
    private UserIdentity buildIdentity() {
        User u = new User();
        u.setId(IdUtil.next());

        UserIdentity i = new UserIdentity();
        i.setId(IdUtil.next());
        i.setRemoteId(RandomStringUtils.randomAlphanumeric(10));
        i.setUser(u);

        return i;
    }

    /**
     * Build some test scopes.
     *
     * @return Test scopes.
     */
    private SortedMap<String, ApplicationScope> buildScopes() {
        SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        Arrays.stream(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
                .forEach(i -> {
                    ApplicationScope a = new ApplicationScope();
                    a.setId(IdUtil.next());
                    a.setName(RandomStringUtils.randomAlphanumeric(10));
                    scopes.put(a.getName(), a);
                });
        return scopes;
    }

    /**
     * Assert that a simple constructor creates an invalid token.
     */
    @Test
    public void testBasicConstructor() {
        IntrospectionResponseEntity entity = new IntrospectionResponseEntity();

        assertNotNull(entity);
        assertFalse(entity.isActive());
        assertNull(entity.getClientId());
        assertNull(entity.getTokenType());
        assertNull(entity.getIat());
        assertNull(entity.getJti());
        assertNull(entity.getNbf());
        assertNull(entity.getAud());
        assertNull(entity.getIss());
        assertNull(entity.getScope());
        assertNull(entity.getSub());
        assertNull(entity.getUsername());
        assertNull(entity.getExp());
    }

    /**
     * Assert that we can convert a bearer token with an identity.
     */
    @Test
    public void testIdentityBearerConstructor() {
        OAuthToken token = buildToken();
        token.setTokenType(OAuthTokenType.Bearer);
        token.setScopes(buildScopes());
        token.setIdentity(buildIdentity());

        IntrospectionResponseEntity entity =
                new IntrospectionResponseEntity(token);

        // Checks valid for all token types.
        assertEquals(token.getClient().getId(), entity.getClientId());
        assertEquals(token.getTokenType(), entity.getTokenType());
        assertEquals(token.getCreatedDate(), entity.getIat());
        assertEquals(!token.isExpired(), entity.isActive());
        assertEquals(token.getId(), entity.getJti());
        assertEquals(token.getCreatedDate(), entity.getNbf());
        assertEquals(token.getClient().getApplication().getId(),
                entity.getAud());
        assertEquals(entity.getIss(), token.getIssuer());

        Calendar expires = (Calendar) token.getCreatedDate().clone();
        expires.add(Calendar.SECOND, token.getExpiresIn().intValue());
        assertEquals(expires, entity.getExp());

        // Create the scope list and compare.
        String scopes = StringUtil.sameOrDefault(entity.getScope(), "");
        List<String> scopeList = Arrays.asList(scopes.split(" "));
        List<String> tokenScopes = new ArrayList<>(token.getScopes().keySet());
        assertEquals(scopeList, tokenScopes);

        assertEquals(token.getIdentity().getRemoteId(), entity.getUsername());
        assertEquals(token.getIdentity().getUser().getId(), entity.getSub());
    }

    /**
     * Assert that we can convert a bearer token with an identity.
     */
    @Test
    public void testClientBearerConstructor() {
        OAuthToken token = buildToken();
        token.getClient().setType(ClientType.ClientCredentials);
        token.setTokenType(OAuthTokenType.Bearer);
        token.setScopes(buildScopes());

        IntrospectionResponseEntity entity
                = new IntrospectionResponseEntity(token);

        // Checks valid for all token types.
        assertEquals(token.getClient().getId(), entity.getClientId());
        assertEquals(token.getTokenType(), entity.getTokenType());
        assertEquals(token.getCreatedDate(), entity.getIat());
        assertEquals(!token.isExpired(), entity.isActive());
        assertEquals(token.getId(), entity.getJti());
        assertEquals(token.getCreatedDate(), entity.getNbf());
        assertEquals(token.getClient().getApplication().getId(),
                entity.getAud());
        assertEquals(entity.getIss(), token.getIssuer());

        Calendar expires = (Calendar) token.getCreatedDate().clone();
        expires.add(Calendar.SECOND, token.getExpiresIn().intValue());
        assertEquals(expires, entity.getExp());

        // Create the scope list and compare.
        String scopes = StringUtil.sameOrDefault(entity.getScope(), "");
        List<String> scopeList = Arrays.asList(scopes.split(" "));
        List<String> tokenScopes = new ArrayList<>(token.getScopes().keySet());
        assertEquals(scopeList, tokenScopes);

        assertNull(entity.getUsername());
        assertEquals(token.getClient().getId(), entity.getSub());
    }

    /**
     * Assert that we can convert a refresh token.
     */
    @Test
    public void testRefreshConstructor() {
        OAuthToken token = buildToken();
        token.setTokenType(OAuthTokenType.Refresh);
        token.setScopes(buildScopes());
        token.setIdentity(buildIdentity());

        IntrospectionResponseEntity entity
                = new IntrospectionResponseEntity(token);

        // Checks valid for all token types.
        assertEquals(token.getClient().getId(), entity.getClientId());
        assertEquals(token.getTokenType(), entity.getTokenType());
        assertEquals(token.getCreatedDate(), entity.getIat());
        assertEquals(!token.isExpired(), entity.isActive());
        assertEquals(token.getId(), entity.getJti());
        assertEquals(token.getCreatedDate(), entity.getNbf());
        assertEquals(token.getClient().getApplication().getId(),
                entity.getAud());
        assertEquals(entity.getIss(), token.getIssuer());

        Calendar expires = (Calendar) token.getCreatedDate().clone();
        expires.add(Calendar.SECOND, token.getExpiresIn().intValue());
        assertEquals(expires, entity.getExp());

        // Create the scope list and compare.
        String scopes = StringUtil.sameOrDefault(entity.getScope(), "");
        List<String> scopeList = Arrays.asList(scopes.split(" "));
        List<String> tokenScopes = new ArrayList<>(token.getScopes().keySet());
        assertEquals(scopeList, tokenScopes);

        assertEquals(token.getIdentity().getRemoteId(), entity.getUsername());
        assertEquals(token.getIdentity().getUser().getId(), entity.getSub());
    }

    /**
     * Assert that we can convert an auth code.
     */
    @Test
    public void testAuthCodeConstructor() {
        OAuthToken token = buildToken();
        token.setTokenType(OAuthTokenType.Authorization);
        token.setScopes(buildScopes());
        token.setIdentity(buildIdentity());

        IntrospectionResponseEntity entity
                = new IntrospectionResponseEntity(token);

        // Checks valid for all token types.
        assertEquals(token.getClient().getId(), entity.getClientId());
        assertEquals(token.getTokenType(), entity.getTokenType());
        assertEquals(token.getCreatedDate(), entity.getIat());
        assertEquals(!token.isExpired(), entity.isActive());
        assertEquals(token.getId(), entity.getJti());
        assertEquals(token.getCreatedDate(), entity.getNbf());
        assertEquals(token.getClient().getApplication().getId(),
                entity.getAud());
        assertEquals(entity.getIss(), token.getIssuer());

        Calendar expires = (Calendar) token.getCreatedDate().clone();
        expires.add(Calendar.SECOND, token.getExpiresIn().intValue());
        assertEquals(expires, entity.getExp());

        // Create the scope list and compare.
        String scopes = StringUtil.sameOrDefault(entity.getScope(), "");
        List<String> scopeList = Arrays.asList(scopes.split(" "));
        List<String> tokenScopes = new ArrayList<>(token.getScopes().keySet());
        assertEquals(scopeList, tokenScopes);

        assertEquals(token.getIdentity().getRemoteId(), entity.getUsername());
        assertEquals(token.getIdentity().getUser().getId(), entity.getSub());
    }

    /**
     * Assert that we can convert a null input.
     */
    @Test
    public void testNullConstructor() {
        IntrospectionResponseEntity entity =
                new IntrospectionResponseEntity(null);

        assertNotNull(entity);
        assertFalse(entity.isActive());
        assertNull(entity.getClientId());
        assertNull(entity.getTokenType());
        assertNull(entity.getIat());
        assertNull(entity.getJti());
        assertNull(entity.getNbf());
        assertNull(entity.getAud());
        assertNull(entity.getIss());
        assertNull(entity.getScope());
        assertNull(entity.getSub());
        assertNull(entity.getUsername());
        assertNull(entity.getExp());
    }

    /**
     * Assert that we can convert an expired token.
     */
    @Test
    public void testExpiredConstructor() {
        OAuthToken token = buildToken();
        token.setTokenType(OAuthTokenType.Bearer);
        token.setScopes(buildScopes());
        token.setIdentity(buildIdentity());

        Calendar then = Calendar.getInstance();
        then.add(Calendar.MONTH, -12);
        token.setCreatedDate(then);
        token.setModifiedDate(then);
        assertTrue(token.isExpired());

        IntrospectionResponseEntity entity =
                new IntrospectionResponseEntity(token);

        assertNotNull(entity);
        assertFalse(entity.isActive());
        assertNull(entity.getClientId());
        assertNull(entity.getTokenType());
        assertNull(entity.getIat());
        assertNull(entity.getJti());
        assertNull(entity.getNbf());
        assertNull(entity.getAud());
        assertNull(entity.getIss());
        assertNull(entity.getScope());
        assertNull(entity.getSub());
        assertNull(entity.getUsername());
        assertNull(entity.getExp());
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and
     * doesn't
     * carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {
        OAuthToken token = buildToken();

        token.setTokenType(OAuthTokenType.Bearer);
        token.setScopes(buildScopes());
        token.setIdentity(buildIdentity());

        IntrospectionResponseEntity entity
                = new IntrospectionResponseEntity(token);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        String output = m.writeValueAsString(entity);
        JsonNode node = m.readTree(output);

        assertEquals(entity.isActive(),
                node.get("active").asBoolean());
        assertEquals(entity.getScope(),
                node.get("scope").asText());
        assertEquals(IdUtil.toString(entity.getClientId()),
                node.get("client_id").asText());
        assertEquals(entity.getUsername(),
                node.get("username").asText());
        assertEquals(entity.getTokenType().toString(),
                node.get("token_type").asText());
        assertEquals(entity.getExp().getTimeInMillis() / 1000,
                node.get("exp").asLong());
        assertEquals(entity.getIat().getTimeInMillis() / 1000,
                node.get("iat").asLong());
        assertEquals(entity.getNbf().getTimeInMillis() / 1000,
                node.get("nbf").asLong());
        assertEquals(IdUtil.toString(entity.getSub()),
                node.get("sub").asText());
        assertEquals(IdUtil.toString(entity.getAud()),
                node.get("aud").asText());
        assertEquals(entity.getIss(),
                node.get("iss").asText());
        assertEquals(IdUtil.toString(entity.getJti()),
                node.get("jti").asText());
    }
}
