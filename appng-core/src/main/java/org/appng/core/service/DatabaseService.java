/*
 * Copyright 2011-2017 the original author or authors.
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.SiteApplication;
import org.appng.core.repository.DatabaseConnectionRepository;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.Datasource;
import org.appng.xml.application.DatasourceType;
import org.appng.xml.application.Datasources;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service offering methods for creating and migrating {@link DatabaseConnection}s of a {@link SiteApplication}.
 * 
 * @author Matthias Müller
 */
public class DatabaseService extends MigrationService {

	private static final String PARAM_PASSWORD = "<password>";
	private static final String PARAM_USER = "<user>";
	private static final String PARAM_DATABASE = "<database>";

	private static final String SCRIPT_INIT = "init.sql";
	private static final String SCRIPT_DROP = "drop.sql";

	private static final String UNDERSCORE = "_";
	private static final String ENC_UTF_8 = "UTF-8";
	private static final String TABLE_CAT = "TABLE_CAT";

	private static final String MIGRATION_PATH = "db/init/";

	@Autowired
	protected DatabaseConnectionRepository databaseConnectionRepository;

	private MigrationStatus migrateSchema(DatabaseConnection rootConnection, String dbInfo,
			SiteApplication siteApplication, Datasource datasource, File sqlFolder, String databasePrefix) {
		Site site = siteApplication.getSite();
		Application application = siteApplication.getApplication();
		DatasourceType type = datasource.getType();
		DatabaseType databaseType = DatabaseType.valueOf(type.name());
		log.info("connected to {} ({})", rootConnection.getJdbcUrl(), dbInfo);
		try {
			DatabaseConnection applicationConnection = createApplicationConnection(site, application, rootConnection,
					databasePrefix);
			String databaseName = applicationConnection.getName();
			if (dataBaseExists(rootConnection, databaseName)) {
				log.info("database '{}' already exists!", databaseName);
			} else {
				DataSource dataSource = getDataSource(rootConnection);
				JdbcOperations operation = new JdbcTemplate(dataSource);
				List<String> sqlScriptLines = getScript(databaseType, SCRIPT_INIT);
				for (String statement : sqlScriptLines) {
					String password = new String(applicationConnection.getPassword());
					String sqlScript = StringUtils.replaceEach(statement,
							new String[] { PARAM_DATABASE, PARAM_USER, PARAM_PASSWORD },
							new String[] { databaseName, applicationConnection.getUserName(), password });
					operation.execute(sqlScript);
				}
				log.info("created database at {}", applicationConnection.getJdbcUrl());
				log.info("created user {}", applicationConnection.getUserName());
			}

			siteApplication.setDatabaseConnection(applicationConnection);
			return migrateApplication(sqlFolder, applicationConnection);
		} catch (Exception e) {
			log.error("an error ocured while migrating the schema", e);
		}
		return MigrationStatus.ERROR;
	}

	private boolean dataBaseExists(DatabaseConnection databaseConnection, String databaseName) {
		Connection connection = null;
		try {
			connection = databaseConnection.getConnection();
			return checkDatabaseExists(connection, databaseName);
		} catch (Exception e) {
			log.warn("error while checking existence of database '" + databaseName + "'", e);
		} finally {
			databaseConnection.closeConnection(connection);
		}
		return false;
	}

	@Transactional
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
	 *            a {@link SiteApplication}
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

	MigrationStatus migrateApplication(File sqlFolder, DatabaseConnection databaseConnection) {
		if (null != databaseConnection) {
			if (databaseConnection.testConnection(null)) {
				String typeFolder = databaseConnection.getType().name().toLowerCase();
				File scriptFolder = new File(sqlFolder.getAbsolutePath(), typeFolder);
				String jdbcUrl = databaseConnection.getJdbcUrl();
				log.info("starting database migration for {} from {}", jdbcUrl, scriptFolder.getAbsolutePath());
				Flyway flyway = new Flyway();
				flyway.setLocations("filesystem:" + scriptFolder.getAbsolutePath());
				return migrate(flyway, databaseConnection);
			} else {
				return MigrationStatus.ERROR;
			}
		}
		return MigrationStatus.NO_DB_SUPPORTED;
	}

	private List<String> getScript(DatabaseType type, String name) throws IOException, URISyntaxException {
		String resourcePath = MIGRATION_PATH + type.name().toLowerCase() + "/" + name;
		InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
		return IOUtils.readLines(is, ENC_UTF_8);
	}

	private String generatePassword(String databaseName) {
		String newPassword = StringUtils.capitalize(databaseName);
		newPassword = StringUtils.reverse(newPassword);
		int i = 1;
		while (newPassword.indexOf(UNDERSCORE) > 0) {
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
		DatabaseType type = databaseConnection.getType();
		DatabaseConnection rootConnection = getRootConnectionOfType(databaseConnection.getType());
		if (rootConnection.isManaged()) {
			if (rootConnection.testConnection(null)) {
				try {
					DataSource dataSource = getDataSource(rootConnection.getJdbcUrl(), rootConnection.getUserName(),
							new String(rootConnection.getPassword()));
					JdbcOperations operation = new JdbcTemplate(dataSource);
					String databaseName = databaseConnection.getName();
					String user = databaseConnection.getUserName();
					List<String> sqlScriptLines = getScript(type, SCRIPT_DROP);
					for (String statement : sqlScriptLines) {
						String sqlScript = StringUtils.replaceEach(statement,
								new String[] { PARAM_DATABASE, PARAM_USER }, new String[] { databaseName, user });
						operation.execute(sqlScript);
					}
					log.info("dropped database at {}", databaseConnection.getJdbcUrl());
					log.info("dropped user  {}", user);
					return MigrationStatus.DB_MIGRATED;
				} catch (Exception e) {
					log.error("error while dropping database " + databaseConnection.getName(), e);
				}
				return MigrationStatus.ERROR;
			} else {
				return MigrationStatus.DB_SUPPORTED;
			}
		} else {
			log.info("{} is not managed by appNG", databaseConnection);
		}
		return MigrationStatus.DB_SUPPORTED;
	}

	/**
	 * Configures the appNG root {@link DatabaseConnection}s by either creating a new one or updating the existing
	 * {@link DatabaseConnection} for each {@link DatabaseType}. Also checks if the connections are working and calls
	 * {@code DatabaseConnection#setActive(false)} if this is not the case.
	 * 
	 * @param rootConnection
	 *            the current root connection
	 * @param changeManagedState
	 *            if set to {@code true}, the managed state for an existing connection is set to
	 *            {@code rootConnection#isManaged()}
	 */
	@Transactional
	public void setActiveConnection(DatabaseConnection rootConnection, boolean changeManagedState) {
		DatabaseType rootType = rootConnection.getType();
		DatabaseConnection conn = getRootConnectionOfType(rootType);
		if (conn == null) {
			conn = rootConnection;
			conn.setName(DATABASE_NAME_PREFIX + rootType.name());
			conn.setDescription(APP_NG_ROOT_DATABASE);
			databaseConnectionRepository.save(conn);
			log.debug("creating new connection: {}", conn);
		} else {
			conn.setJdbcUrl(rootConnection.getJdbcUrl());
			conn.setDriverClass(rootConnection.getDriverClass());
			conn.setUserName(rootConnection.getUserName());
			conn.setPassword(rootConnection.getPassword());
			if (changeManagedState) {
				conn.setManaged(rootConnection.isManaged());
			}
			log.debug("updating existing connection: {}", conn);
		}
		setConnectionActive(conn);

		for (DatabaseType type : DatabaseType.values()) {
			if (!type.equals(rootType)) {
				DatabaseConnection connection = getRootConnectionOfType(type);
				if (connection == null) {
					log.debug("initializing connection of type {}", type);
					connection = new DatabaseConnection(type, DATABASE_NAME_PREFIX + type.name(), "user", new byte[0]);
					connection.setName(DATABASE_NAME_PREFIX + type.name());
					connection.setDescription(APP_NG_ROOT_DATABASE);
					databaseConnectionRepository.save(connection);
					log.debug("creating new connection: {}", connection);
					setConnectionActive(connection);
				} else if (connection.isActive()) {
					setConnectionActive(connection);
				} else {
					log.debug("connection {} is inactive", connection);
				}
			}
		}
	}

	private void setConnectionActive(DatabaseConnection connection) {
		StringBuilder dbInfo = new StringBuilder();
		if (connection.testConnection(dbInfo)) {
			connection.setActive(true);
			log.info("{} ({}) is active.", connection.toString(), dbInfo);
		} else {
			connection.setActive(false);
			log.info("{} is not working and will be deactivated.", connection.toString());
		}
	}

	private DatabaseConnection getRootConnectionOfType(DatabaseType type) {
		return databaseConnectionRepository.findBySiteIsNullAndType(type);
	}

	/**
	 * Migrates the database for the given {@link SiteApplication}.
	 * 
	 * @param siteApplication
	 *            the {@link SiteApplication} to migrate the database for
	 * @param applicationInfo
	 *            the {@link Application}'s {@link ApplicationInfo} as read from
	 *            {@value org.appng.api.model.ResourceType#APPLICATION_XML_NAME}.
	 * @param sqlFolder
	 *            the root folder for the migration-scripts provided by the {@link SiteApplication}
	 * @return the {@link MigrationService.MigrationStatus}
	 */
	public MigrationStatus manageApplicationConnection(SiteApplication siteApplication, ApplicationInfo applicationInfo,
			File sqlFolder, String databasePrefix) {
		Datasources datasources = applicationInfo.getDatasources();
		MigrationStatus status = MigrationStatus.NO_DB_SUPPORTED;
		if (null != datasources) {
			if (!datasources.getDatasource().isEmpty()) {
				for (Datasource datasource : datasources.getDatasource()) {
					DatasourceType datasourceType = datasource.getType();
					DatabaseType databaseType = DatabaseType.valueOf(datasourceType.name());
					DatabaseConnection rootConnection = getRootConnectionOfType(databaseType);
					if (rootConnection.isActive()) {
						if (rootConnection.isManaged()) {
							StringBuilder dbInfo = new StringBuilder();
							if (rootConnection.testConnection(dbInfo)) {
								return migrateSchema(rootConnection, dbInfo.toString(), siteApplication, datasource,
										sqlFolder, databasePrefix);
							} else {
								status = MigrationStatus.DB_NOT_AVAILABLE;
								log.warn("the connection '{}' using '{}' does not work", rootConnection.getName(),
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
						log.info("connection {} is inactive, skipping", rootConnection.toString());
					}
				}
			}
		}
		return status;
	}

	/**
	 * Persists the given {@link DatabaseConnection}.
	 * 
	 * @param databaseConnection
	 *            the connection to persist.
	 */
	@Transactional
	public void save(DatabaseConnection databaseConnection) {
		databaseConnectionRepository.save(databaseConnection);
	}

}
