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
import net.krotscheck.kangaroo.common.hibernate.entity.ICreatedDateEntity;
import net.krotscheck.kangaroo.common.hibernate.entity.IModifiedDateEntity;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
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
public final class HttpSession
        implements ICreatedDateEntity, IModifiedDateEntity, Cloneable {

    /**
     * A 64-byte id that identifies this particular session.
     */
    @Id
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".BigIntegerType")
    @GenericGenerator(name = "secure_random_bytes",
            parameters = @Parameter(name = "byteCount", value = "64"),
            strategy = "net.krotscheck.kangaroo.authz.common.database.util"
                    + ".SecureRandomIdGenerator")
    @GeneratedValue(generator = "secure_random_bytes")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private BigInteger id = null;

    /**
     * The date this record was created.
     */
    @Column(name = "createdDate")
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".CalendarTimestampType")
    private Calendar createdDate;

    /**
     * The date this record was last modified.
     */
    @Column(name = "modifiedDate")
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type"
            + ".CalendarTimestampType")
    private Calendar modifiedDate;

    /**
     * OAuth tokens attached to this session.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "httpSession",
            cascade = {CascadeType.REMOVE, CascadeType.MERGE},
            orphanRemoval = true
    )
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<OAuthToken> refreshTokens = new ArrayList<>();

    /**
     * Timeout.
     */
    @Basic(optional = false)
    @Column(name = "sessionTimeout", nullable = false)
    private long sessionTimeout = -1;

    /**
     * Set the ID.
     *
     * @return The ID for this session.
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Get the ID.
     *
     * @param id The ID for this session.
     */
    public void setId(final BigInteger id) {
        this.id = id;
    }

    /**
     * Get the date on which this record was created.
     *
     * @return The created date.
     */
    public Calendar getCreatedDate() {
        if (createdDate == null) {
            return null;
        } else {
            return (Calendar) createdDate.clone();
        }
    }

    /**
     * Set the date on which this record was created.
     *
     * @param date The creation date for this entity.
     */
    public void setCreatedDate(final Calendar date) {
        this.createdDate = (Calendar) date.clone();
    }

    /**
     * Get the last modified date.
     *
     * @return The last time this record was modified, or null.
     */
    public Calendar getModifiedDate() {
        if (modifiedDate == null) {
            return null;
        } else {
            return (Calendar) modifiedDate.clone();
        }
    }

    /**
     * Set the last modified date.
     *
     * @param date The modified date for this entity.
     */
    public void setModifiedDate(final Calendar date) {
        this.modifiedDate = (Calendar) date.clone();
    }

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

    /**
     * Equality implementation, global.
     *
     * @param o The object to test.
     * @return True if the ID's are equal, otherwise false.
     */
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !this.getClass().isInstance(o)) {
            return false;
        }

        // Cast
        HttpSession other = (HttpSession) o;

        // if the id is missing, return false
        if (id == null) {
            return false;
        }

        // equivalence by id
        return id.equals(other.getId());
    }

    /**
     * Public Hashcode generation.
     *
     * @return A hashcode for this entity.
     */
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getId())
                .append(this.getClass().getName())
                .toHashCode();
    }

    /**
     * Simplified Stringification.
     *
     * @return A string representation of the instance.
     */
    public String toString() {
        return String.format("%s [id=%s]", this.getClass().getCanonicalName(),
                getId());
    }

    /**
     * Clone this instance.
     *
     * @return A clone of this entity.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
