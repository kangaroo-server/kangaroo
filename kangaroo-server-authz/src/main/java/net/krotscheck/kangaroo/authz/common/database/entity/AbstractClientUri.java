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
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.net.URI;

/**
 * This represents a redirect URL attached to a specific client.
 *
 * @author Michael Krotscheck
 */
@MappedSuperclass
public abstract class AbstractClientUri extends AbstractAuthzEntity {

    /**
     * The client this redirect is associated to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(
            dataType = "string",
            example = "3f631a2d6a04f5cc55f9e192f45649b7"
    )
    private Client client;

    /**
     * The redirect URL.
     */
    @Basic(optional = false)
    @Column(name = "uri")
    @Type(type = "net.krotscheck.kangaroo.common.hibernate.type.URIType")
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    private URI uri;

    /**
     * Get the Client for this state.
     *
     * @return This state's Client.
     */
    public final Client getClient() {
        return client;
    }

    /**
     * Set a new Client.
     *
     * @param client The new Client.
     */
    public final void setClient(final Client client) {
        this.client = client;
    }

    /**
     * Get the validated client redirection URI.
     *
     * @return The URI which the client requested a result response to.
     */
    public final URI getUri() {
        return uri;
    }

    /**
     * Set a new redirection URI.
     *
     * @param redirect The redirection URI.
     */
    public final void setUri(final URI redirect) {
        this.uri = redirect;
    }

    /**
     * The owner of this entity.
     *
     * @return This entity's owner, if it exists.
     */
    @Override
    @Transient
    @JsonIgnore
    public final User getOwner() {
        if (client != null) {
            return client.getOwner();
        }
        return null;
    }
}
