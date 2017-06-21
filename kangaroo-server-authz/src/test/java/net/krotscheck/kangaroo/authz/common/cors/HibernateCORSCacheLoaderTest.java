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

import net.krotscheck.kangaroo.authz.admin.v1.test.rule.TestDataResource;
import net.krotscheck.kangaroo.test.DatabaseTest;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;

/**
 * Unit test for the CORS cache loader.
 *
 * @author Michael Krotscheck
 */
public final class HibernateCORSCacheLoaderTest extends DatabaseTest {

    /**
     * Preload data into the system.
     */
    @ClassRule
    public static final TestDataResource TEST_DATA_RESOURCE =
            new TestDataResource(HIBERNATE_RESOURCE);

    /**
     * Assert that we can load valid domains.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void loadValid() throws Exception {
        TEST_DATA_RESOURCE.getAdminApplication()
                .getBuilder()
                .referrer("https://valid.example.com")
                .build();

        URI valid = new URI("https://valid.example.com");
        HibernateCORSCacheLoader loader =
                new HibernateCORSCacheLoader(getSessionFactory());

        Boolean result = loader.load(valid);
        Assert.assertTrue(result);
    }

    /**
     * Assert that we can load invalid domains.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void loadInvalid() throws Exception {
        URI invalid = new URI("https://invalid.example.com");
        HibernateCORSCacheLoader loader =
                new HibernateCORSCacheLoader(getSessionFactory());

        Boolean result = loader.load(invalid);
        Assert.assertFalse(result);
    }

    /**
     * Assert that we don't die if the database throws errors.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void loadWithHibernateException() throws Exception {
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Session mockSession = Mockito.mock(Session.class);
        Mockito.doReturn(mockSession).when(mockFactory).openSession();
        Mockito.doThrow(HibernateException.class).when(mockSession)
                .beginTransaction();

        URI invalid = new URI("https://invalid.example.com");
        HibernateCORSCacheLoader loader =
                new HibernateCORSCacheLoader(mockFactory);

        Boolean result = loader.load(invalid);
        Assert.assertFalse(result);
    }

    /**
     * Assert that exceptions thrown in the final catch block are recast.
     * This is really done for coverage.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = Exception.class)
    public void loadWithGenericException() throws Exception {
        SessionFactory mockFactory = Mockito.mock(SessionFactory.class);
        Session mockSession = Mockito.mock(Session.class);
        Mockito.doReturn(mockSession).when(mockFactory).openSession();
        Mockito.doThrow(Exception.class).when(mockSession)
                .beginTransaction();

        URI invalid = new URI("https://invalid.example.com");
        HibernateCORSCacheLoader loader =
                new HibernateCORSCacheLoader(mockFactory);

        Boolean result = loader.load(invalid);
        Assert.assertFalse(result);
    }
}
