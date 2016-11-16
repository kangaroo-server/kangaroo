/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.common.exception.exception.HttpNotFoundException;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.util.SortUtil;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.QueryBuilder;

import java.util.List;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A RESTful API that permits the management of user resources.
 *
 * @author Michael Krotscheck
 */
@Path("/user")
@RolesAllowed(Scope.USER)
@OAuth2
public final class UserService extends AbstractService {

    /**
     * Create a new instance of the user service.
     *
     * @param session         The Hibernate session.
     * @param searchFactory   The FT Search factory.
     * @param fullTextSession The fulltext search factory.
     */
    @Inject
    public UserService(final Session session,
                       final SearchFactory searchFactory,
                       final FullTextSession fullTextSession) {
        super(session, searchFactory, fullTextSession);
    }

    /**
     * Search the users in the system.
     *
     * @param offset      The offset of the first users to fetch.
     * @param limit       The number of data sets to fetch.
     * @param queryString The search term for the query.
     * @return A list of search results.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUsers(
            @DefaultValue("0") @QueryParam("offset")
            final Integer offset,
            @DefaultValue("10") @QueryParam("limit")
            final Integer limit,
            @DefaultValue("") @QueryParam("q")
            final String queryString) {

        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(User.class)
                .get();

        org.apache.lucene.search.Query luceneQuery = builder
                .keyword()
                .fuzzy()
                .onFields("identities.claims", "identities.remoteId")
                .matching(queryString)
                .createQuery();

        FullTextQuery query =
                getFullTextSession()
                        .createFullTextQuery(luceneQuery, User.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        List results = query.list();
        Integer size = query.getResultSize();

        return ListResponseBuilder
                .builder()
                .offset(offset)
                .limit(limit)
                .addResult(results)
                .total(size)
                .build();
    }

    /**
     * Returns a list of all users.
     *
     * @param offset Paging offset, in records.
     * @param limit  The number of records to return.
     * @param sort   The field on which the records should be sorted.
     * @param order  The sort order, ASC or DESC.
     * @return A response with the results of the passed parameters.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response browseUsers(
            @QueryParam(ApiParam.OFFSET_QUERY)
            @DefaultValue(ApiParam.OFFSET_DEFAULT)
            final int offset,
            @QueryParam(ApiParam.LIMIT_QUERY)
            @DefaultValue(ApiParam.LIMIT_DEFAULT)
            final int limit,
            @QueryParam(ApiParam.SORT_QUERY)
            @DefaultValue(ApiParam.SORT_DEFAULT)
            final String sort,
            @QueryParam(ApiParam.ORDER_QUERY)
            @DefaultValue(ApiParam.ORDER_DEFAULT)
            final SortOrder order) {

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession().createCriteria(User.class);
        countCriteria.setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession().createCriteria(User.class);
        browseCriteria.setFirstResult(offset);
        browseCriteria.setMaxResults(limit);
        browseCriteria.addOrder(SortUtil.order(order, sort));

        return ListResponseBuilder.builder()
                .offset(offset)
                .limit(limit)
                .order(order)
                .sort(sort)
                .total(countCriteria.uniqueResult())
                .addResult(browseCriteria.list())
                .build();
    }

    /**
     * Returns a specific user.
     *
     * @param id The Unique Identifier for the user.
     * @return A response with the user that was requested.
     */
    @GET
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("id") final UUID id) {
        User user = getSession().get(User.class, id);
        if (user == null) {
            throw new HttpNotFoundException();
        }
        return Response.ok(user).build();
    }
}
