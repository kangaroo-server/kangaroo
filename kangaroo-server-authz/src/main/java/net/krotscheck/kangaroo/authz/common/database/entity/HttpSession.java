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

package net.krotscheck.kangaroo.authz.common.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * This hibernate entity describes an HTTP session that maintains 'refresh
 * token' references on behalf of the user. It's used to permit renewing a
 * user's token, without forcing them to log in multiple times.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "http_sessions")
public final class HttpSession extends AbstractEntity {

    /**
     * OAuth tokens attached to this session.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "httpSession")
    @JsonIgnore
    private List<OAuthToken> refreshTokens = new ArrayList<>();

    /**
     * Timeout.
     */
    @Basic(optional = false)
    @Column(name = "sessionTimeout", nullable = false)
    private long sessionTimeout = -1;

    /**
     * Return a {@link List} of refreshTokens.
     *
     * @return the refresh tokens associated with this session.
     */
    public List<OAuthToken> getRefreshTokens() {
        return refreshTokens;
    }

    /**
     * Set the list of tokens.
     *
     * @param refreshTokens The refresh tokens attached to this session.
     */
    public void setRefreshTokens(final List<OAuthToken> refreshTokens) {
        this.refreshTokens = new ArrayList<>(refreshTokens);
    }

    /**
     * Get the session timeout.
     *
     * @return Timeout, in seconds.
     */
    public long getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Set the session timeout, in seconds.
     *
     * @param sessionTimeout The new session timeout, in seconds.
     */
    public void setSessionTimeout(final long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

}
