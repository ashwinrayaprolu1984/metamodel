/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.metamodel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.jdbc.JdbcDataContext;
import org.apache.metamodel.jdbc.JdbcTestTemplates;
import org.apache.metamodel.query.Query;
import org.apache.metamodel.schema.Schema;
import org.apache.metamodel.schema.Table;

/**
 * DB2 integration test. This is a read-only integration test, meant to be
 * modified for whatever server is available (even within Human Inference).
 */
public class DB2Test extends TestCase {

    private static final String URL = "jdbc:db2://TODO:50000/TODO";

    private static final String USERNAME = "TODO";
    private static final String PASSWORD = "TODO";
    private Connection _connection;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        _connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        _connection.close();
    }
    
    
    public void testInterpretationOfNull() throws Exception {
        JdbcTestTemplates.interpretationOfNulls(_connection);
    }

    public void testDefaultSchema() throws Exception {
        JdbcDataContext dc = new JdbcDataContext(_connection);
        Schema schema = dc.getDefaultSchema();
        assertEquals(USERNAME.toUpperCase(), schema.getName());

        Table countryTable = schema.getTableByName("COUNTRY");
        assertNotNull(countryTable);

        DataSet ds = dc.query().from(countryTable).selectCount().execute();
        assertTrue(ds.next());
        assertEquals("Row[values=[1008]]", ds.getRow().toString());
        assertFalse(ds.next());
        ds.close();
    }

    public void testMaxRowsOnly() throws Exception {
        JdbcDataContext dc = new JdbcDataContext(_connection);
        Schema schema = dc.getDefaultSchema();
        String[] tableNames = schema.getTableNames();
        System.out.println("Tables: " + Arrays.toString(tableNames));

        Table countryTable = schema.getTableByName("COUNTRY");
        assertNotNull(countryTable);

        Query query = dc.query().from(countryTable).select("COUNTRYCODE").limit(200).toQuery();
        assertEquals("SELECT DB2INST1.\"COUNTRY\".\"COUNTRYCODE\" FROM DB2INST1.\"COUNTRY\" "
                + "FETCH FIRST 200 ROWS ONLY", dc.getQueryRewriter().rewriteQuery(query));

        DataSet ds = dc.executeQuery(query);
        for (int i = 0; i < 200; i++) {
            assertTrue(ds.next());
            assertEquals(1, ds.getRow().getValues().length);
        }
        assertFalse(ds.next());
        ds.close();
    }

    public void testMaxRowsAndOffset() throws Exception {
        JdbcDataContext dc = new JdbcDataContext(_connection);
        Schema schema = dc.getDefaultSchema();
        String[] tableNames = schema.getTableNames();
        System.out.println("Tables: " + Arrays.toString(tableNames));

        Table countryTable = schema.getTableByName("COUNTRY");
        assertNotNull(countryTable);

        Query query = dc.query().from(countryTable).select("COUNTRYCODE").limit(200).offset(200).toQuery();
        assertEquals(
                "SELECT metamodel_subquery.\"COUNTRYCODE\" FROM ("
                        + "SELECT DB2INST1.\"COUNTRY\".\"COUNTRYCODE\", ROW_NUMBER() OVER() AS metamodel_row_number FROM DB2INST1.\"COUNTRY\""
                        + ") metamodel_subquery WHERE metamodel_row_number BETWEEN 201 AND 400", dc.getQueryRewriter()
                        .rewriteQuery(query));

        DataSet ds = dc.executeQuery(query);
        for (int i = 0; i < 200; i++) {
            assertTrue(ds.next());
            assertEquals(1, ds.getRow().getValues().length);
        }
        assertFalse(ds.next());
        ds.close();
    }
}
