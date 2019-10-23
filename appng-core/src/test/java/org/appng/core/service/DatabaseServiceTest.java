/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.core.service;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.repository.DatabaseConnectionRepository;
import org.appng.core.repository.config.HikariCPConfigurer;
import org.hsqldb.jdbc.JDBCDriver;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PlatformTestConfig.class, initializers = TestInitializer.class)
@DirtiesContext
public class DatabaseServiceTest extends TestInitializer {

	@Autowired
	DatabaseService databaseService;

	@Autowired
	DatabaseConnectionRepository databaseConnectionRepository;

	@Test
	public void testInitDatabase() throws Exception {
		String jdbcUrl = "jdbc:hsqldb:mem:testInitDatabase";
		Properties platformProperties = getProperties(DatabaseType.HSQL, jdbcUrl, "sa", "", JDBCDriver.class.getName());
		DatabaseConnection platformConnection = databaseService.initDatabase(platformProperties);
		StringBuilder dbInfo = new StringBuilder();
		Assert.assertTrue(platformConnection.testConnection(dbInfo, true));
		Assert.assertTrue(dbInfo.toString().startsWith("HSQL Database Engine"));
		String rootName = "appNG Root Database";
		Assert.assertEquals(rootName, platformConnection.getDescription());
		Assert.assertEquals(DatabaseType.HSQL, platformConnection.getType());
		validateSchemaVersion(platformConnection, "4.1.1");

		DatabaseConnection mssql = new DatabaseConnection(DatabaseType.MSSQL, rootName, "", "".getBytes());
		mssql.setName(rootName);
		mssql.setActive(false);
		databaseConnectionRepository.save(mssql);

		databaseService.setActiveConnection(platformConnection, false);

		List<DatabaseConnection> connections = databaseConnectionRepository.findAll();
		Assert.assertEquals(3, connections.size());

		for (DatabaseConnection connection : connections) {
			switch (connection.getType()) {
			case HSQL:
				Assert.assertTrue(connection.isActive());
				break;
			case MSSQL:
				Assert.assertFalse(connection.isActive());
				break;
			case MYSQL:
				Assert.assertFalse(connection.isActive());
				break;
			}
		}

	}

	@Test
	@Ignore("run locally")
	public void testInitDatabaseMysql() throws Exception {
		String jdbcUrl = "jdbc:mysql://localhost:3306/appng_migration";
		String user = "user";
		String password = "password";
		Properties platformProperties = getProperties(DatabaseType.MYSQL, jdbcUrl, user, password,
				DatabaseType.MYSQL.getDefaultDriver());
		DatabaseConnection platformConnection = databaseService.initDatabase(platformProperties);
		StringBuilder dbInfo = new StringBuilder();
		Assert.assertTrue(platformConnection.testConnection(dbInfo, true));
		Assert.assertTrue(dbInfo.toString().startsWith("MySQL 5.6"));
		Assert.assertEquals("appNG Root Database", platformConnection.getDescription());
		Assert.assertEquals(DatabaseType.MYSQL, platformConnection.getType());
		Assert.assertTrue(platformConnection.getDatabaseSize() > 0.0d);
		validateSchemaVersion(platformConnection, "4.0.0");
	}

	@Test
	@Ignore("run locally")
	public void testInitDatabaseMssql() throws Exception {
		String jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=appng_migration";
		String user = "user";
		String password = "password";
		Properties platformProperties = getProperties(DatabaseType.MSSQL, jdbcUrl, user, password,
				DatabaseType.MSSQL.getDefaultDriver());
		DatabaseConnection platformConnection = databaseService.initDatabase(platformProperties);
		StringBuilder dbInfo = new StringBuilder();
		Assert.assertTrue(platformConnection.testConnection(dbInfo, true));
		Assert.assertTrue(dbInfo.toString().startsWith("Microsoft SQL Server"));
		Assert.assertEquals("appNG Root Database", platformConnection.getDescription());
		Assert.assertEquals(DatabaseType.MSSQL, platformConnection.getType());
		Assert.assertTrue(platformConnection.getDatabaseSize() > 0.0d);
		validateSchemaVersion(platformConnection, "4.0.0");
		DataSource sqlDataSource = new HikariCPConfigurer(platformConnection).getDataSource();
		DatabaseMetaData metaData = sqlDataSource.getConnection().getMetaData();
		Assert.assertTrue(metaData.getDatabaseProductName().startsWith("Microsoft SQL Server"));
	}

	private Properties getProperties(DatabaseType databaseType, String jdbcUrl, String user, String password,
			String driverClass) {
		Properties platformProperties = new Properties();
		platformProperties.setProperty(DatabaseService.DATABASE_TYPE, databaseType.name());
		platformProperties.setProperty(DatabaseService.HIBERNATE_CONNECTION_URL, jdbcUrl);
		platformProperties.setProperty(DatabaseService.HIBERNATE_CONNECTION_USERNAME, user);
		platformProperties.setProperty(DatabaseService.HIBERNATE_CONNECTION_PASSWORD, password);
		platformProperties.setProperty(DatabaseService.DATABASE_VALIDATION_QUERY, "");
		platformProperties.setProperty(DatabaseService.DATABASE_VALIDATION_PERIOD, "15");
		platformProperties.setProperty(DatabaseService.HIBERNATE_CONNECTION_DRIVER_CLASS, driverClass);
		return platformProperties;
	}

	private void validateSchemaVersion(DatabaseConnection connection, String version) throws SQLException {
		Assert.assertEquals(version, databaseService.status(connection).getVersion().toString());
	}

	@Test
	public void testUserName() {
		Application app = Mockito.mock(Application.class);
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getId()).thenReturn(1234);
		Mockito.when(app.getId()).thenReturn(1234);
		String userName = databaseService.getUserName(site, app);
		Assert.assertTrue(userName.length() <= 16);
	}

}
