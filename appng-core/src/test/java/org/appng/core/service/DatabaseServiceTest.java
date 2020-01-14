/*
 * Copyright 2011-2020 the original author or authors.
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.SiteImpl;
import org.appng.core.repository.DatabaseConnectionRepository;
import org.hsqldb.jdbc.JDBCDriver;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

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
		validateSchemaVersion(platformConnection, "4.1.2");

		DatabaseConnection mssql = new DatabaseConnection(DatabaseType.MSSQL, rootName, "", "".getBytes());
		mssql.setName(rootName);
		mssql.setActive(false);
		databaseConnectionRepository.save(mssql);

		databaseService.setActiveConnection(platformConnection, false);

		List<DatabaseConnection> connections = databaseConnectionRepository.findAll();
		Assert.assertEquals(4, connections.size());

		for (DatabaseConnection connection : connections) {
			switch (connection.getType()) {
			case HSQL:
				Assert.assertTrue(connection.isActive());
				break;
			default:
				Assert.assertFalse(connection.isActive());
				break;
			}
		}

	}

	@Test
	@Ignore("uses testcontainers, which needs docker")
	public void testInitDatabaseMySql56() throws Exception {
		testInitDatabaseMySql("5.6");
	}

	@Test
	@Ignore("uses testcontainers, which needs docker")
	public void testInitDatabaseMySql57() throws Exception {
		testInitDatabaseMySql("5.7");
	}

	@Test
	@Ignore("uses testcontainers, which needs docker")
	public void testInitDatabaseMySql8() throws Exception {
		testInitDatabaseMySql("8");
	}

	void testInitDatabaseMySql(String version) throws Exception {
		try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:" + version)) {
			mysql.withUsername("root").withPassword("").start();
			validateConnectionType(mysql, DatabaseType.MYSQL, "MySQL", version, true, true);
		}
	}

	@Test
	@Ignore("uses testcontainers, which needs docker")
	public void testInitDatabasePostgreSQL10() throws Exception {
		testInitDatabasePostgreSQL("10.8");
	}

	@Test
	@Ignore("uses testcontainers, which needs docker")
	public void testInitDatabasePostgreSQL11() throws Exception {
		testInitDatabasePostgreSQL("11.3");
	}

	void testInitDatabasePostgreSQL(String version) throws Exception {
		try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:" + version)) {
			postgres.start();
			validateConnectionType(postgres, DatabaseType.POSTGRESQL, "PostgreSQL", version, true, true);
		}
	}

	@Test
	@Ignore("uses testcontainers, which needs docker")
	public void testInitDatabaseMsSql() throws Exception {
		try (MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>()) {
			mssql.start();
			validateConnectionType(mssql, DatabaseType.MSSQL, "Microsoft SQL Server", "14.00", false, false);
		}
	}

	private void validateConnectionType(JdbcDatabaseContainer<?> container, DatabaseType databaseType,
			String productName, String productVersion, boolean checksize, boolean checkConnection)
			throws SQLException, IOException, URISyntaxException {
		Properties platformProperties = getProperties(databaseType, container.getJdbcUrl(), container.getUsername(),
				container.getPassword(), databaseType.getDefaultDriver());
		DatabaseConnection platformConnection = databaseService.initDatabase(platformProperties);
		StringBuilder dbInfo = new StringBuilder();
		Assert.assertTrue(platformConnection.testConnection(dbInfo, true));
		Assert.assertTrue(dbInfo.toString(), dbInfo.toString().startsWith(productName + " " + productVersion));
		Assert.assertEquals("appNG Root Database", platformConnection.getDescription());
		Assert.assertEquals(databaseType, platformConnection.getType());
		if (checksize) {
			Assert.assertTrue(platformConnection.getDatabaseSize() > 0.0d);
		}
		validateSchemaVersion(platformConnection, "4.1.2");

		testRootConnectionJPA(platformConnection);
		if (checkConnection) {
			validateCreateAndDropApplicationConnection(platformConnection, container.getFirstMappedPort(),
					checkConnection);
		}
	}

	private void testRootConnectionJPA(DatabaseConnection platformConnection) {
		LocalContainerEntityManagerFactoryBean lcemf = new LocalContainerEntityManagerFactoryBean();
		lcemf.setDataSource(platformConnection.getDataSource());
		lcemf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		lcemf.setPackagesToScan(SiteImpl.class.getPackage().getName());
		lcemf.setJpaDialect(new HibernateJpaDialect());
		lcemf.setPersistenceUnitName("appng-" + platformConnection.getType().name());
		lcemf.afterPropertiesSet();

		EntityManagerFactory emf = lcemf.getObject();
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		SiteImpl site = new SiteImpl();
		site.setName("localhost");
		site.setHost("localhost");
		site.setDomain("http://localhost:8080");
		em.persist(site);
		em.getTransaction().commit();
		Assert.assertNotNull(site.getId());
		Assert.assertNotNull(site.getVersion());
		em.close();
		emf.close();
		lcemf.destroy();
	}

	private void validateCreateAndDropApplicationConnection(DatabaseConnection platformConnection, Integer port,
			boolean checkConnection) throws IOException, URISyntaxException {
		DatabaseType type = platformConnection.getType();
		String jdbcUrl = type.getTemplateUrl().replace("<name>", "appng_database")
				.replace(type.getDefaultPort().toString(), port.toString());
		DatabaseConnection applicationConnection = new DatabaseConnection(type, jdbcUrl, type.getDefaultDriver(),
				"appng_user", "appng_password42".getBytes(), type.getDefaultValidationQuery());
		applicationConnection.setName("appng_database");
		databaseService.initApplicationConnection(applicationConnection, platformConnection.getDataSource());
		if (checkConnection) {
			Assert.assertTrue(applicationConnection.testConnection(null));
		}

		databaseService.dropApplicationConnection(applicationConnection, platformConnection.getDataSource());
		Assert.assertFalse(applicationConnection.testConnection(null));
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
