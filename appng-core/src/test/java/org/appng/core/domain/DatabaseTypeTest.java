/*
 * Copyright 2011-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.core.domain;

import static org.appng.core.domain.DatabaseConnection.DatabaseType.HSQL;
import static org.appng.core.domain.DatabaseConnection.DatabaseType.MSSQL;
import static org.appng.core.domain.DatabaseConnection.DatabaseType.MYSQL;

import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.junit.Assert;
import org.junit.Test;

public class DatabaseTypeTest {

	@Test
	public void testHsql() {
		runTest(HSQL, "jdbc:hsqldb:hsql://localhost:9001/appng");
	}

	@Test
	public void testMySql() {
		runTest(MYSQL, "jdbc:mysql://localhost:3306/appng");
		Assert.assertEquals("com.mysql.cj.jdbc.Driver", MYSQL.getDefaultDriver());
		Assert.assertEquals("com.mysql.cj.jdbc.MysqlDataSource", MYSQL.getDataSourceClassName());
	}

	@Test
	public void testMsSql() {
		runTest(MSSQL, "jdbc:sqlserver://localhost:1433;databaseName=appng");
	}

	@Test
	public void testGetDatabaseConnectionString() {
		DatabaseConnection con = new DatabaseConnection(MYSQL, "jdbc:mysql://localhost:3306/appng?foo=bar",
				DatabaseType.MYSQL.getDefaultDriver(), null, null, null);
		Assert.assertEquals("appng", con.getDatabaseName());
		Assert.assertEquals("jdbc:mysql://localhost:3306/app-database?foo=bar",
				con.getDatabaseConnectionString("app-database"));
	}

	private void runTest(DatabaseType type, String jdbcUrl) {
		Assert.assertEquals("appng", type.getDatabaseName(jdbcUrl));
		Assert.assertEquals("appng", type.getDatabaseName(jdbcUrl + "?foo=bar"));
	}

}