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

package net.krotscheck.kangaroo.authz.common.authenticator.oauth2;

import com.google.common.base.Strings;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.MisconfiguredAuthenticatorException;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.ThirdPartyErrorException;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.InvalidRequestException;
import net.krotscheck.kangaroo.authz.oauth2.exception.RFC6749.ServerErrorException;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract class which manages the standard OAuth2 Authorization code
 * flow, to use as an IdP. If your IdP is reasonably compliant, you can
 * extend this class and provide any missing parameters.
 *
 * @author Michael Krotscheck
 */
public abstract class AbstractOAuth2Authenticator implements IAuthenticator {

    /**
     * Map reference, used for decoding.
     */
    public static final GenericType<HashMap<String, String>> MAP_TYPE =
            new GenericType<HashMap<String, String>>() {
            };

    /**
     * The config storage key for the facebook client id.
     */
    public static final String CLIENT_ID_KEY = "client_id";

    /**
     * The config storage key for the facebook client secret.
     */
    public static final String CLIENT_SECRET_KEY = "client_secret";

    /**
     * HTTP Client, used for API calls.
     */
    @Inject
    private Client client;

    /**
     * The database session.
     */
    @Inject
    private Session session;

    /**
     * Get the HTTP client.
     *
     * @return The HTTP client.
     */
    public final Client getClient() {
        return client;
    }

    /**
     * Set the HTTP client.
     *
     * @param client The HTTP client.
     */
    public final void setClient(final Client client) {
        this.client = client;
    }

    /**
     * Get the session.
     *
     * @return The hibernate session.
     */
    public final Session getSession() {
        return session;
    }

    /**
     * Set the hibernate session.
     *
     * @param session The new hibernate session.
     */
    public final void setSession(final Session session) {
        this.session = session;
    }

    /**
     * The path to the /authorization endpoint.
     *
     * @return The fully qualified URL to the OAuth2 authorization endpoint.
     */
    protected abstract String getAuthEndpoint();

    /**
     * The path to the /token endpoint.
     *
     * @return The fully qualified URL to the OAuth2 token request endpoint.
     */
    protected abstract String getTokenEndpoint();

    /**
     * The string listing out all the scopes required.
     *
     * @return A list of scopes, using the IdP appropriate separator character.
     */
    protected abstract String getScopes();

    /**
     * This method returns the original URI, without any query or fragment
     * parameters. It is used to reframe the redirect contract provided by
     * the authorization service.
     *
     * @param uri The URI to manipulate.
     * @return The previous URI, without any query or fragment.
     */
    private URI buildRedirect(final URI uri) {
        return UriBuilder.fromUri(uri)
                .fragment(null)
                .replaceQuery(null)
                .build();
    }

    /**
     * Provided with an authenticator, extract the client secret.
     *
     * @param authenticator The authenticator.
     * @return The client secret.
     */
    protected final String getClientSecret(final Authenticator authenticator) {
        return authenticator.getConfiguration().get(CLIENT_SECRET_KEY);
    }

    /**
     * Provided with an authenticator, provide the client id.
     *
     * @param authenticator The authenticator.
     * @return The OAuth2 client id.
     */
    protected final String getClientId(final Authenticator authenticator) {
        return authenticator.getConfiguration().get(CLIENT_ID_KEY);
    }

    /**
     * Delegate an authentication request to the oauth2 IdP.
     *
     * @param authenticator The authenticator configuration.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return A response indicating the initial request to facebeook.
     */
    @Override
    public final Response delegate(final Authenticator authenticator,
                                   final URI callback) {
        // Validate the authenticator
        validate(authenticator);

        if (callback == null) {
            throw new ServerErrorException();
        }

        // Extract the state, since OAuth2 requires it be sent separately,
        String state = HttpUtil.parseQueryParams(callback)
                .getFirst("state");
        if (Strings.isNullOrEmpty(state)) {
            throw new ServerErrorException();
        }
        URI redirect = buildRedirect(callback);

        String clientId = getClientId(authenticator);

        UriBuilder b = UriBuilder
                .fromUri(getAuthEndpoint())
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirect)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam("scope", getScopes());

        return Response.status(Status.FOUND)
                .location(b.build())
                .build();
    }

    /**
     * Validate that the configuration values provided are correct.
     *
     * @param authenticator The authenticator configuration.
     * @throws KangarooException Thrown if we cannot use these values.
     */
    @Override
    public final void validate(final Authenticator authenticator)
            throws KangarooException {

        if (authenticator == null || authenticator.getConfiguration() == null) {
            throw new MisconfiguredAuthenticatorException();
        }
        Map<String, String> config = authenticator.getConfiguration();

        String clientId = config.getOrDefault(CLIENT_ID_KEY, null);
        String clientSecret = config.getOrDefault(CLIENT_SECRET_KEY, null);

        if (Strings.isNullOrEmpty(clientSecret)
                || Strings.isNullOrEmpty(clientId)) {
            throw new MisconfiguredAuthenticatorException();
        }
    }

    /**
     * Handle the response from the IdP.
     *
     * @param authenticator The authenticator configuration.
     * @param parameters    Parameters for the authenticator, retrieved from
     *                      an appropriate source.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return A user identity, or a runtime error that will be sent back.
     */
    @Override
    public final UserIdentity authenticate(final Authenticator authenticator,
                                           final MultivaluedMap<String, String>
                                                   parameters,
                                           final URI callback) {
        URI redirect = buildRedirect(callback);

        // Is this an error response?
        if (parameters.containsKey("error")) {
            throw new ThirdPartyErrorException(parameters);
        }

        // We're expecting the authorization code.
        String code = parameters.getFirst("code");
        if (Strings.isNullOrEmpty(code)) {
            throw new InvalidRequestException();
        }

        // Retrieve the access token from the token endpoint.
        String clientId = getClientId(authenticator);
        String clientSecret = getClientSecret(authenticator);

        OAuth2IdPToken token = resolveIdPToken(clientId, clientSecret,
                code, redirect);
        if (Strings.isNullOrEmpty(token.getAccessToken())) {
            throw new ThirdPartyErrorException();
        }

        OAuth2User oUser = loadUserIdentity(token);
        if (Strings.isNullOrEmpty(oUser.getId())) {
            throw new ThirdPartyErrorException();
        }

        // Try to find an identity.
        Application application = authenticator.getClient().getApplication();
        Criteria criteria = session.createCriteria(UserIdentity.class)
                .createAlias("user", "u")
                .add(Restrictions.eq("type", authenticator.getType()))
                .add(Restrictions.eq("remoteId", oUser.getId()))
                .add(Restrictions.eq("u.application", application))
                .setFirstResult(0)
                .setMaxResults(1);
        UserIdentity identity = (UserIdentity) criteria.uniqueResult();

        // See if this is a brand new user.
        if (identity == null) {
            User u = new User();
            u.setApplication(application);
            u.setRole(application.getDefaultRole());

            identity = new UserIdentity();
            identity.setUser(u);
            identity.setType(authenticator.getType());
            identity.setRemoteId(oUser.getId());
            identity.setClaims(oUser.getClaims());

            session.save(u);
            session.save(identity);
        } else {
            identity.getClaims().putAll(oUser.getClaims());
            session.save(identity);
        }

        // Query for the user id in this application.
        return identity;
    }

    /**
     * Provided a (presumably valid) OAuth2 token, retrieve the user's remote
     * identity from the IdP.
     *
     * @param token The OAuth token.
     * @return The user identity.
     */
    protected abstract OAuth2User loadUserIdentity(OAuth2IdPToken token);

    /**
     * Resolve an authorization code to an oauth token.
     *
     * @param clientId          The client id.
     * @param clientSecret      The client secret.
     * @param authorizationCode The authorization code.
     * @param redirectUrl       The redirect URL.
     * @return An OAuth2 token entity.
     */
    private OAuth2IdPToken resolveIdPToken(final String clientId,
                                           final String clientSecret,
                                           final String authorizationCode,
                                           final URI redirectUrl) {
        String authHeader = HttpUtil.authHeaderBasic(clientId, clientSecret);

        // Build the request payload
        Form f = new Form();
        f.param("client_id", clientId);
        f.param("client_secret", clientSecret);
        f.param("code", authorizationCode);
        f.param("grant_type", "authorization_code");
        f.param("redirect_uri", redirectUrl.toString());
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        // Issue the request
        Response r = client.target(getTokenEndpoint())
                .request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(postEntity);
        try {
            // If this is an error...
            if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                return r.readEntity(OAuth2IdPToken.class);
            } else {
                Map<String, String> params = r.readEntity(MAP_TYPE);
                throw new ThirdPartyErrorException(params);
            }
        } catch (ProcessingException e) {
            throw new ThirdPartyErrorException();
        } finally {
            r.close();
        }
    }
}
