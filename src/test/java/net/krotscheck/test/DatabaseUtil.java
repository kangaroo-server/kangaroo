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

package net.krotscheck.test;

import org.dbunit.IDatabaseTester;
import org.dbunit.JndiDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A test suite that sets up a database for the services to run against.
 *
 * @author Michael Krotscheck
 */
public abstract class DatabaseUtil {

    /**
     * Manually construct the hibernate configuration.
     */
    private static Configuration config;

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);

    /**
     * Singleton instance of the database tester.
     */
    private static IDatabaseTester tester;

    /**
     * Load some test data into our database.
     *
     * @param testData The test data xml file to map.
     */
    public static void loadTestData(final File testData) {
        try {
            logger.info("Loading test data...");

            IDatabaseTester databaseTester = getDatabaseTester();
            FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
            IDataSet dataSet = builder.build(testData);

            databaseTester.setDataSet(dataSet);
            databaseTester.onSetup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clears the test data.
     *
     * @param testData The test data xml file to map.
     */
    public static void clearTestData(final File testData) {
        try {
            logger.info("Clearing test data...");

            IDatabaseTester databaseTester = getDatabaseTester();
            FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
            IDataSet dataSet = builder.build(testData);

            // initialize your dataset here
            databaseTester.setDataSet(dataSet);
            databaseTester.onTearDown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an instance of the database tester.
     *
     * @return The database tester.
     * @throws Exception Misc migration exceptions.
     */
    private static IDatabaseTester getDatabaseTester() throws Exception {
        if (tester == null) {
            tester = new JndiDatabaseTester("java:/comp/env/jdbc/OIDServerDB");
            tester.setSetUpOperation(DatabaseOperation.DELETE_ALL);
            tester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
            tester.setTearDownOperation(DatabaseOperation.DELETE_ALL);
        }

        return tester;
    }

}
