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

package net.krotscheck.kangaroo.authz.admin.v1.resource;

import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.runner.ParameterizedSingleInstanceTestRunner.ParameterizedSingleInstanceTestRunnerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

/**
 * Test the CRUD methods of the scope service.
 *
 * @param <K> The type of parent entity for this test.
 * @param <T> The type of entity to execute this test for.
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
@UseParametersRunnerFactory(ParameterizedSingleInstanceTestRunnerFactory.class)
public abstract class AbstractSubserviceCRUDTest<K extends AbstractAuthzEntity,
        T extends AbstractAuthzEntity> extends AbstractServiceCRUDTest<T> {

    /**
     * Class reference for this class' parent type, used in casting.
     */
    private final Class<K> parentClass;

    /**
     * Create a new instance of this parameterized test.
     *
     * @param parentClass   The raw parent type, used for type-based parsing.
     * @param childClass    The raw child type, used for type-based parsing.
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public AbstractSubserviceCRUDTest(final Class<K> parentClass,
                                      final Class<T> childClass,
                                      final ClientType clientType,
                                      final String tokenScope,
                                      final Boolean createUser,
                                      final Boolean shouldSucceed) {
        super(childClass, clientType, tokenScope, createUser, shouldSucceed);
        this.parentClass = parentClass;
    }

    /**
     * Return the correct parent entity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    protected abstract K getParentEntity(ApplicationContext context);

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected final T createValidEntity(final ApplicationContext context) {
        K parent = getParentEntity(context);
        return createValidEntity(context, parent);
    }

    /**
     * Given a parent entity and a context, create a valid entity.
     *
     * @param context The environment context.
     * @param parent  The parent entity.
     * @return A valid entity.
     */
    protected abstract T createValidEntity(ApplicationContext context,
                                           K parent);

    /**
     * Create a valid parent entity for the given context.
     *
     * @param context The environment context.
     * @return A valid entity.
     */
    protected abstract K createParentEntity(ApplicationContext context);

    /**
     * Assert that we cannot read from an different parent entity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testGetDifferentParent() throws Exception {
        T originalEntity = getEntity(getAdminContext());
        K testParent = (K) getParentEntity(getSecondaryContext()).clone();

        T testEntity = createValidEntity(getAdminContext(), testParent);
        testEntity.setId(originalEntity.getId());

        // Issue the request.
        Response r = getEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that we cannot read from an invalid parent entity.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testGetInvalidParent() throws Exception {
        T originalEntity = (T) getEntity(getAdminContext()).clone();
        K testParent = createParentEntity(getAdminContext());
        testParent.setId(UUID.randomUUID());

        T testEntity = createValidEntity(getAdminContext(), testParent);
        testEntity.setId(originalEntity.getId());

        // Issue the request.
        Response r = getEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that an entity cannot be created for a nonexistent parent.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPostInvalidParent() throws Exception {
        K testParent = createParentEntity(getAdminContext());
        testParent.setId(UUID.randomUUID());
        T testEntity = createValidEntity(getAdminContext(), testParent);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that an entity can only be modified if linked to its actual
     * parent.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPutDifferentParent() throws Exception {
        K testParent = getParentEntity(getSecondaryContext());
        T testEntity = createValidEntity(getAdminContext(), testParent);
        T validEntity = getEntity(getAdminContext());
        testEntity.setId(validEntity.getId());

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that an entity cannot be updated for a nonexistent parent.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testPutInvalidParent() throws Exception {
        K testParent = createParentEntity(getAdminContext());
        testParent.setId(UUID.randomUUID());
        T testEntity = createValidEntity(getAdminContext(), testParent);
        T validEntity = getEntity(getAdminContext());
        testEntity.setId(validEntity.getId());

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that an entity cannot be deleted for a different parent.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testDeleteDifferentParent() throws Exception {
        K testParent = getParentEntity(getSecondaryContext());
        T testEntity = createValidEntity(getAdminContext(), testParent);
        T validEntity = getEntity(getAdminContext());
        testEntity.setId(validEntity.getId());

        // Issue the request.
        Response r = deleteEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }

    /**
     * Assert that an entity cannot be deleted for a nonexistent parent.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public final void testDeleteInvalidParent() throws Exception {
        K testParent = createParentEntity(getAdminContext());
        testParent.setId(UUID.randomUUID());
        T testEntity = createValidEntity(getAdminContext(), testParent);
        T validEntity = getEntity(getAdminContext());
        testEntity.setId(validEntity.getId());

        // Issue the request.
        Response r = deleteEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }
}
