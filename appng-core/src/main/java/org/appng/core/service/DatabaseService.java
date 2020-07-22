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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.SiteApplication;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.repository.DatabaseConnectionRepository;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.Datasource;
import org.appng.xml.application.DatasourceType;
import org.appng.xml.application.Datasources;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * A service offering methods for creating and migrating {@link DatabaseConnection}s of a {@link SiteApplication}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class DatabaseService extends MigrationService {

	private static final String PARAM_PASSWORD = "<password>";
	private static final String PARAM_USER = "<user>";
	private static final String PARAM_DATABASE = "<database>";

	private static final String SCRIPT_INIT = "init.sql";
	private static final String SCRIPT_DROP = "drop.sql";

	private static final String UNDERSCORE = "_";
	private static final String TABLE_CAT = "TABLE_CAT";

	private static final String MIGRATION_PATH = "db/init/";

	@Autowired
	protected DatabaseConnectionRepository databaseConnectionRepository;

	private MigrationStatus migrateSchemaForApplication(DatabaseConnection rootConnection, String dbInfo,
			SiteApplication application, File sqlFolder, String databasePrefix) {
		LOGGER.info("connected to {} ({})", rootConnection.getJdbcUrl(), dbInfo);
		try {
			DatabaseConnection applicationConnection = createApplicationConnection(application.getSite(),
					application.getApplication(), rootConnection, databasePrefix);
			String databaseName = applicationConnection.getName();
			if (dataBaseExists(rootConnection, databaseName)) {
				LOGGER.info("database '{}' already exists!", databaseName);
			} else {
				initApplicationConnection(applicationConnection, getDataSource(rootConnection));
			}

			application.setDatabaseConnection(applicationConnection);
			if (StringUtils.isNotBlank(application.getApplication().getProperties()
					.getString(ApplicationProperties.FLYWAY_MIGRATION_PACKAGE))) {
				return MigrationStatus.DB_NOT_MIGRATED;
			}
			return migrateApplication(sqlFolder, application, databasePrefix);
		} catch (Exception e) {
			LOGGER.error("an error ocured while migrating the schema", e);
		}
		return MigrationStatus.ERROR;
	}

	protected void initApplicationConnection(DatabaseConnection applicationConnection, DataSource dataSource)
			throws IOException, URISyntaxException {
		executeSqlScript(applicationConnection, dataSource, SCRIPT_INIT);
		LOGGER.info("created database at {}", applicationConnection.getJdbcUrl());
		LOGGER.info("created user {}", applicationConnection.getUserName());
	}

	private void executeSqlScript(DatabaseConnection applicationConnection, DataSource dataSource, String scriptName)
			throws IOException, URISyntaxException {
		JdbcOperations operation = new JdbcTemplate(dataSource);
		List<String> sqlScriptLines = getScript(applicationConnection.getType(), scriptName);
		for (String statement : sqlScriptLines) {
			String password = new String(applicationConnection.getPassword());
			String sqlScript = StringUtils.replaceEach(statement,
					new String[] { PARAM_DATABASE, PARAM_USER, PARAM_PASSWORD },
					new String[] { applicationConnection.getName(), applicationConnection.getUserName(), password });
			operation.execute(sqlScript);
		}
	}

	private boolean dataBaseExists(DatabaseConnection databaseConnection, String databaseName) {
		Connection connection = null;
		try {
			connection = databaseConnection.getConnection();
			return checkDatabaseExists(connection, databaseName);
		} catch (Exception e) {
			LOGGER.warn(String.format("error while checking existence of database '%s'", databaseName), e);
		} finally {
			databaseConnection.closeConnection(connection);
		}
		return false;
	}

	private DatabaseConnection createApplicationConnection(Site site, Application application,
			DatabaseConnection rootConnection, String databasePrefix) {
		DatabaseConnection databaseConnection = new DatabaseConnection();
		configureApplicationConnection(site, application, rootConnection, databaseConnection, databasePrefix);
		databaseConnectionRepository.save(databaseConnection);
		return databaseConnection;
	}

	/**
	 * Resets the configuration for the {@link DatabaseConnection} (if existing) of the given {@link SiteApplication} to
	 * the default values.
	 * 
	 * @param siteApplication
	 *                        a {@link SiteApplication}
	 */
	public void resetApplicationConnection(SiteApplication siteApplication, String databasePrefix) {
		DatabaseConnection databaseConnection = siteApplication.getDatabaseConnection();
		DatabaseType type = databaseConnection.getType();
		DatabaseConnection rootConnection = getRootConnectionOfType(type);
		if (rootConnection.isManaged()) {
			configureApplicationConnection(siteApplication.getSite(), siteApplication.getApplication(), rootConnection,
					databaseConnection, databasePrefix);
		}
	}

	private void configureApplicationConnection(Site site, Application application, DatabaseConnection rootConnection,
			DatabaseConnection databaseConnection, String databasePrefix) {
		String databaseName = getDatabaseName(site, application, databasePrefix);
		String newURL = rootConnection.getDatabaseConnectionString(databaseName);
		setConnectionProperties(site, application, databaseConnection, rootConnection.getType(), databaseName, newURL);
		databaseConnection.setValidationQuery(rootConnection.getValidationQuery());
	}

	private DatabaseConnection configureApplicationConnection(SiteApplication siteApplication, Datasource datasource,
			String databasePrefix) {
		Site site = siteApplication.getSite();
		Application application = siteApplication.getApplication();
		DatabaseConnection databaseConnection = new DatabaseConnection();
		DatabaseType type = DatabaseType.valueOf(datasource.getType().name());
		String databaseName = getDatabaseName(site, application, databasePrefix);
		String newURL = type.getTemplateUrl().replace(DatabaseConnection.DB_PLACEHOLDER, databaseName);

		setConnectionProperties(site, application, databaseConnection, type, databaseName, newURL);
		return databaseConnection;
	}

	private void setConnectionProperties(Site site, Application application, DatabaseConnection databaseConnection,
			DatabaseType type, String databaseName, String newURL) {
		String newUser = getUserName(site, application);
		String newPassword = generatePassword(databaseName);

		databaseConnection.setDriverClass(type.getDefaultDriver());
		databaseConnection.setJdbcUrl(newURL);
		databaseConnection.setUserName(newUser);
		databaseConnection.setPassword(newPassword.getBytes());
		databaseConnection.setName(databaseName);
		databaseConnection.setType(type);
		databaseConnection.setValidationQuery(type.getDefaultValidationQuery());
		databaseConnection.setDescription(site.getName() + " - " + application.getName());
		databaseConnection.setSite(site);
	}

	protected String getUserName(Site site, Application application) {
		return "site" + site.getId() + "app" + application.getId();
	}

	private String getDatabaseName(Site site, Application application, String databasePrefix) {
		return (StringUtils.trimToEmpty(databasePrefix) + "appng_" + site.getName().replaceAll("-", UNDERSCORE)
				+ UNDERSCORE + application.getName().replaceAll("-", UNDERSCORE)).toLowerCase();
	}

	MigrationStatus migrateApplication(File sqlFolder, SiteApplication siteApplication, String dataBasePrefix) {
		DatabaseConnection databaseConnection = siteApplication.getDatabaseConnection();
		if (null != databaseConnection) {
			if (databaseConnection.testConnection(null)) {
				String typeFolder = databaseConnection.getType().name().toLowerCase();
				File scriptFolder = new File(sqlFolder.getAbsolutePath(), typeFolder);
				String jdbcUrl = databaseConnection.getJdbcUrl();
				LOGGER.info("starting database migration for {} from {}", jdbcUrl, scriptFolder.getAbsolutePath());

				List<String> locations = new ArrayList<>();
				locations.add(Location.FILESYSTEM_PREFIX + scriptFolder.getAbsolutePath());
				String flywayMigrationPackage = siteApplication.getApplication().getProperties()
						.getString(ApplicationProperties.FLYWAY_MIGRATION_PACKAGE);
				if (StringUtils.isNotBlank(flywayMigrationPackage)) {
					LOGGER.info("adding Flyway migration package '{}' for application {}", flywayMigrationPackage,
							siteApplication.getApplication().getName());
					locations.add(flywayMigrationPackage);
				}
				Flyway flyway = getFlyway(databaseConnection, siteApplication.getSite().getSiteClassLoader(),
						locations.toArray(new String[0]));
				return migrate(flyway, databaseConnection);
			} else {
				return MigrationStatus.ERROR;
			}
		} else {
			return manageApplicationConnection(siteApplication, sqlFolder, dataBasePrefix);
		}
	}

	private List<String> getScript(DatabaseType type, String name) throws IOException, URISyntaxException {
		String resourcePath = MIGRATION_PATH + type.name().toLowerCase() + "/" + name;
		InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
		return IOUtils.readLines(is, StandardCharsets.UTF_8);
	}

	private String generatePassword(String databaseName) {
		String newPassword = StringUtils.capitalize(databaseName);
		newPassword = StringUtils.reverse(newPassword);
		int i = 1;
		while (newPassword.contains(UNDERSCORE)) {
			newPassword = StringUtils.replaceOnce(newPassword, UNDERSCORE, String.valueOf(i++));
			newPassword = StringUtils.capitalize(newPassword);
		}
		return newPassword;
	}

	private boolean checkDatabaseExists(Connection connection, String database) throws SQLException {
		ResultSet catalogs = connection.getMetaData().getCatalogs();
		while (catalogs.next()) {
			if (catalogs.getString(TABLE_CAT).toLowerCase().equals(database.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	MigrationStatus dropDataBaseAndUser(DatabaseConnection databaseConnection) {
		DatabaseConnection rootConnection = getRootConnectionOfType(databaseConnection.getType());
		if (rootConnection.isManaged()) {
			if (rootConnection.testConnection(null)) {
				try {
					DataSource dataSource = getDataSource(rootConnection.getJdbcUrl(), rootConnection.getUserName(),
							new String(rootConnection.getPassword()));
					dropApplicationConnection(databaseConnection, dataSource);
					return MigrationStatus.DB_MIGRATED;
				} catch (Exception e) {
					LOGGER.error(String.format("error while dropping database %s", databaseConnection.getName()), e);
				}
				return MigrationStatus.ERROR;
			} else {
				return MigrationStatus.DB_SUPPORTED;
			}
		} else {
			LOGGER.info("{} is not managed by appNG", databaseConnection);
		}
		return MigrationStatus.DB_SUPPORTED;
	}

	protected void dropApplicationConnection(DatabaseConnection databaseConnection, DataSource dataSource)
			throws IOException, URISyntaxException {
		executeSqlScript(databaseConnection, dataSource, SCRIPT_DROP);
		LOGGER.info("dropped database at {}", databaseConnection.getJdbcUrl());
		LOGGER.info("dropped user {}", databaseConnection.getUserName());
	}

	/**
	 * Configures and (optionally) migrates the appNG root {@link DatabaseConnection} from the given
	 * {@link java.util.Properties}.
	 * 
	 * @param  config
	 *                   the properties read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 * @param  managed
	 *                   whether to make the connection managed
	 * @param  setActive
	 *                   if the connection should be set as he active root connection, creating a new
	 *                   {@link DatabaseConnection} if necessary. Only applied if {@link #status(DatabaseConnection)}
	 *                   returns a non-null value.
	 * @return           the appNG root {@link DatabaseConnection}
	 */
	@Transactional
	public DatabaseConnection initDatabase(java.util.Properties config, boolean managed, boolean setActive) {
		DatabaseConnection platformConnection = initDatabase(config);
		MigrationInfoService statusComplete = platformConnection.getMigrationInfoService();
		if (setActive && statusComplete != null && null != statusComplete.current()) {
			platformConnection.setManaged(managed);
			platformConnection = setActiveConnection(platformConnection, true);
			platformConnection.setMigrationInfoService(statusComplete);
		}
		return platformConnection;
	}

	/**
	 * Configures the appNG root {@link DatabaseConnection}s by either creating a new one or updating the existing
	 * {@link DatabaseConnection} for each {@link DatabaseType}. Also checks if the connections are working and calls
	 * {@code DatabaseConnection#setActive(false)} if this is not the case.
	 * 
	 * @param rootConnection
	 *                           the current root connection
	 * @param changeManagedState
	 *                           if set to {@code true}, the managed state for an existing connection is set to
	 *                           {@code rootConnection#isManaged()}
	 */
	@Transactional
	public DatabaseConnection setActiveConnection(DatabaseConnection rootConnection, boolean changeManagedState) {
		DatabaseType rootType = rootConnection.getType();
		DatabaseConnection conn = getRootConnectionOfType(rootType);
		if (conn == null) {
			conn = rootConnection;
			conn.setName(DATABASE_NAME_PREFIX + rootType.name());
			conn.setDescription(APP_NG_ROOT_DATABASE);
			databaseConnectionRepository.save(conn);
			LOGGER.debug("creating new connection: {}", conn);
		} else {
			conn.setJdbcUrl(rootConnection.getJdbcUrl());
			conn.setDriverClass(rootConnection.getDriverClass());
			conn.setUserName(rootConnection.getUserName());
			conn.setPassword(rootConnection.getPassword());
			if (changeManagedState) {
				conn.setManaged(rootConnection.isManaged());
			}
			LOGGER.debug("updating existing connection: {}", conn);
		}
		setConnectionActive(conn, false);

		for (DatabaseType type : DatabaseType.values()) {
			if (!type.equals(rootType)) {
				DatabaseConnection connection = getRootConnectionOfType(type);
				if (connection == null) {
					LOGGER.debug("initializing connection of type {}", type);
					connection = new DatabaseConnection(type, DATABASE_NAME_PREFIX + type.name(), "user", new byte[0]);
					connection.setName(DATABASE_NAME_PREFIX + type.name());
					connection.setDescription(APP_NG_ROOT_DATABASE);
					databaseConnectionRepository.save(connection);
					LOGGER.debug("creating new connection: {}", connection);
					setConnectionActive(connection, true);
				} else if (connection.isActive()) {
					setConnectionActive(connection, true);
				} else {
					connection.registerDriver(false);
					LOGGER.debug("connection {} is inactive", connection);
				}
			}
		}
		return conn;
	}

	private void setConnectionActive(DatabaseConnection connection, boolean registerDriver) {
		StringBuilder dbInfo = new StringBuilder();
		if (registerDriver) {
			connection.registerDriver(false);
		}
		if (connection.testConnection(dbInfo)) {
			connection.setActive(true);
			LOGGER.info("{} ({}) is active.", connection.toString(), dbInfo);
		} else {
			connection.setActive(false);
			LOGGER.info("{} is not working and will be deactivated.", connection.toString());
		}
	}

	public DatabaseConnection getRootConnectionOfType(DatabaseType type) {
		return databaseConnectionRepository.findBySiteIsNullAndType(type);
	}

	/**
	 * Migrates the database for the given {@link SiteApplication}.
	 * 
	 * @param  siteApplication
	 *                             the {@link SiteApplication} to migrate the database for
	 * @param  applicationInfo
	 *                             the {@link Application}'s {@link ApplicationInfo} as read from
	 *                             {@value org.appng.api.model.ResourceType#APPLICATION_XML_NAME}.
	 * @param  sqlFolder
	 *                             the root folder for the migration-scripts provided by the {@link SiteApplication}
	 * @return                     the {@link MigrationService.MigrationStatus}
	 */
	@Transactional
	public MigrationStatus manageApplicationConnection(SiteApplication siteApplication, File sqlFolder,
			String databasePrefix) {
		Datasources datasources = siteApplication.getApplication().getResources().getApplicationInfo().getDatasources();
		MigrationStatus status = MigrationStatus.NO_DB_SUPPORTED;
		if (!(null == datasources || datasources.getDatasource().isEmpty())) {
			for (Datasource datasource : datasources.getDatasource()) {
				DatasourceType datasourceType = datasource.getType();
				DatabaseType databaseType = DatabaseType.valueOf(datasourceType.name());
				DatabaseConnection rootConnection = getRootConnectionOfType(databaseType);
				if (rootConnection.isActive()) {
					if (rootConnection.isManaged()) {
						StringBuilder dbInfo = new StringBuilder();
						if (rootConnection.testConnection(dbInfo)) {
							return migrateSchemaForApplication(rootConnection, dbInfo.toString(), siteApplication,
									sqlFolder, databasePrefix);
						} else {
							status = MigrationStatus.DB_NOT_AVAILABLE;
							LOGGER.warn("the connection '{}' using '{}' does not work", rootConnection.getName(),
									rootConnection.getDriverClass());
						}
					} else {
						DatabaseConnection databaseConnection = configureApplicationConnection(siteApplication,
								datasource, databasePrefix);
						save(databaseConnection);
						databaseConnection.setSite(siteApplication.getSite());
						siteApplication.setDatabaseConnection(databaseConnection);
						return MigrationStatus.DB_SUPPORTED;
					}
				} else {
					status = MigrationStatus.DB_NOT_AVAILABLE;
					LOGGER.info("connection {} is inactive, skipping", rootConnection.toString());
				}
			}
		}
		return status;
	}

	/**
	 * Persists the given {@link DatabaseConnection}.
	 * 
	 * @param databaseConnection
	 *                           the connection to persist.
	 */
	@Transactional
	public void save(DatabaseConnection databaseConnection) {
		databaseConnectionRepository.save(databaseConnection);
	}

}
