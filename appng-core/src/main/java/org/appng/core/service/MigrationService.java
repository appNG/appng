/*
 * Copyright 2011-2018 the original author or authors.
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
import java.util.Arrays;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.SiteApplication;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.internal.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * A service offering methods for configuring and initializing {@link DatabaseConnection}s using
 * <a href="http://www.flywaydb.org">Flyway</a>.
 * 
 * @author Matthias Müller
 */
public class MigrationService {

	protected Logger log = LoggerFactory.getLogger(getClass());

	public static final String DATABASE_MIN_CONNECTIONS = "database.minConnections";
	public static final String DATABASE_MAX_CONNECTIONS = "database.maxConnections";
	public static final String DATABASE_NAME = "database.name";
	public static final String DATABASE_REPAIR = "database.repair";
	public static final String DATABASE_TYPE = "database.type";
	protected static final String DATABASE_VALIDATION_QUERY = "database.validationQuery";
	protected static final String DATABASE_VALIDATION_PERIOD = "database.validationPeriod";

	protected static final String HIBERNATE_CONNECTION_PASSWORD = "hibernate.connection.password";
	protected static final String HIBERNATE_CONNECTION_USERNAME = "hibernate.connection.username";
	protected static final String HIBERNATE_CONNECTION_DRIVER_CLASS = "hibernate.connection.driver_class";
	protected static final String HIBERNATE_CONNECTION_URL = "hibernate.connection.url";

	protected static final String APP_NG_ROOT_DATABASE = "appNG Root Database";
	protected static final String DATABASE_NAME_PREFIX = "appNG ";

	private static final String LOCATION_PREFIX = "db.migration.";

	/**
	 * Enum type defining the different states of a database migration.
	 * 
	 * @author Matthias Müller
	 * 
	 */
	public enum MigrationStatus {
		/** no database supported */
		NO_DB_SUPPORTED,
		/** database supported */
		DB_SUPPORTED,
		/** database not available */
		DB_NOT_AVAILABLE,
		/** database migrated */
		DB_MIGRATED,
		/** error */
		ERROR;

		/**
		 * Checks whether this {@link MigrationStatus} is in an erroneous state.
		 * 
		 * @return {@code true} if this {@link MigrationStatus} is one of {@link MigrationStatus#ERROR} or
		 *         {@link MigrationStatus#DB_NOT_AVAILABLE}, {@code false} otherwise
		 */
		public boolean isErroneous() {
			return MigrationStatus.ERROR.equals(this) || MigrationStatus.DB_NOT_AVAILABLE.equals(this);
		}
	}

	/**
	 * Configures and (optionally) migrates the appNG root {@link DatabaseConnection} from the given
	 * {@link java.util.Properties}.
	 * 
	 * @param config
	 *            the properties read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 * @return the appNG root {@link DatabaseConnection}
	 */
	public DatabaseConnection initDatabase(java.util.Properties config) {
		DatabaseConnection platformConnection = getPlatformConnection(config);
		Boolean doRepair = Boolean.valueOf(config.getProperty(DATABASE_REPAIR, "true"));
		initDatabase(platformConnection, doRepair);
		return platformConnection;
	}

	public DatabaseConnection getPlatformConnection(java.util.Properties properties) {
		String type = properties.getProperty(DATABASE_TYPE);
		DatabaseType databaseType = DatabaseType.MYSQL;
		if (StringUtils.isEmpty(type)) {
			log.warn("Property  " + DATABASE_TYPE + " is not specified, using default: " + databaseType.name());
		} else {
			try {
				databaseType = DatabaseType.valueOf(type.toUpperCase());
			} catch (IllegalArgumentException e) {
				log.error("Invalid value for property " + DATABASE_TYPE + ": " + type + " ; must be one of: "
						+ Arrays.asList(DatabaseType.values()));
			}
		}

		String jdbcUrl = properties.getProperty(HIBERNATE_CONNECTION_URL);
		String driverClass = properties.getProperty(HIBERNATE_CONNECTION_DRIVER_CLASS);
		String username = properties.getProperty(HIBERNATE_CONNECTION_USERNAME);
		String password = properties.getProperty(HIBERNATE_CONNECTION_PASSWORD);
		String validationQuery = properties.getProperty(DATABASE_VALIDATION_QUERY);
		Integer validationPeriod = Integer.parseInt(properties.getProperty(DATABASE_VALIDATION_PERIOD));

		DatabaseConnection conn = new DatabaseConnection(databaseType, jdbcUrl, driverClass, username,
				password.getBytes(), validationQuery);
		conn.setName(DATABASE_NAME_PREFIX + databaseType.name());
		conn.setDescription(APP_NG_ROOT_DATABASE);
		conn.setValidationPeriod(validationPeriod);
		return conn;
	}

	/**
	 * Returns the current {@link MigrationInfo} for the connection
	 * 
	 * @param config
	 *            the configuration read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 * @return the current {@link MigrationInfo} for the given connection
	 * @see #status(DatabaseConnection)
	 */
	public MigrationInfo status(java.util.Properties config) {
		return status(getPlatformConnection(config));
	}

	/**
	 * Returns the current {@link MigrationInfo} for the given {@link DatabaseConnection}
	 * 
	 * @param connection
	 *            a {@link DatabaseConnection}
	 * @return the current {@link MigrationInfo} for the given connection (may be {@code null}).
	 * @see MigrationInfoService#current()
	 */
	public MigrationInfo status(DatabaseConnection connection) {
		MigrationInfoService statusComplete = statusComplete(connection);
		if (null != statusComplete) {
			return statusComplete.current();
		}
		return null;
	}

	/**
	 * Returns the current {@link MigrationInfoService} for the given {@link DatabaseConnection} (the appNG root
	 * connection).
	 * 
	 * @param connection
	 *            a {@link DatabaseConnection}
	 * @return the current {@link MigrationInfoService} for the given connection (may be {@code null}).
	 * @see MigrationInfoService
	 */
	public MigrationInfoService statusComplete(DatabaseConnection connection) {
		StringBuilder dbInfo = new StringBuilder();
		if (connection.testConnection(dbInfo)) {
			log.info("connected to " + connection.getJdbcUrl() + " (" + dbInfo.toString() + ")");
			Flyway flyway = new Flyway();
			DataSource dataSource = getDataSource(connection);
			flyway.setDataSource(dataSource);
			String location = LOCATION_PREFIX + connection.getType().name().toLowerCase();
			flyway.setLocations(location);
			return flyway.info();
		} else {
			log.error(connection.toString() + " is not working, unable to retrieve connection status.");
		}
		return null;
	}

	/**
	 * Returns the current {@link MigrationInfoService} for the given {@link DatabaseConnection}, which must be owned by
	 * a {@link SiteApplication}.
	 * 
	 * @param connection
	 *            a {@link DatabaseConnection} owned by a {@link SiteApplication}
	 * @param sqlFolder
	 *            the path to migration scripts
	 * @return the current {@link MigrationInfoService} for the given connection (may be {@code null}).
	 * @see MigrationInfoService
	 */
	public MigrationInfoService statusComplete(DatabaseConnection connection, File sqlFolder) {
		if (null != connection && connection.testConnection(null)) {
			String typeFolder = connection.getType().name().toLowerCase();
			File scriptFolder = new File(sqlFolder.getAbsolutePath(), typeFolder);
			Flyway flyway = new Flyway();
			flyway.setDataSource(getDataSource(connection));
			flyway.setLocations(Location.FILESYSTEM_PREFIX + scriptFolder.getAbsolutePath());
			return flyway.info();
		}
		return null;
	}

	protected MigrationStatus initDatabase(DatabaseConnection rootConnection, Boolean doRepair) {
		StringBuilder dbInfo = new StringBuilder();
		String jdbcUrl = rootConnection.getJdbcUrl();
		if (rootConnection.testConnection(dbInfo, true)) {
			log.info("connected to " + jdbcUrl + " (" + dbInfo.toString() + ")");
			Flyway flyway = new Flyway();
			String location = LOCATION_PREFIX + rootConnection.getType().name().toLowerCase();
			flyway.setLocations(location);
			if (doRepair) {
				flyway.setDataSource(getDataSource(rootConnection));
				flyway.repair();
			}
			return migrate(flyway, rootConnection);
		} else {
			log.error(rootConnection.toString() + " is not working, initializing database was not successful.");
		}
		return MigrationStatus.ERROR;
	}

	protected MigrationStatus migrate(Flyway flyway, DatabaseConnection databaseConnection) {
		String jdbcUrl = databaseConnection.getJdbcUrl();
		try {
			DataSource dataSource = getDataSource(databaseConnection);
			flyway.setDataSource(dataSource);
			flyway.migrate();
			return MigrationStatus.DB_MIGRATED;
		} catch (FlywayException e) {
			log.error("error while migrating " + jdbcUrl, e);
		}
		return MigrationStatus.ERROR;
	}

	protected DataSource getDataSource(DatabaseConnection databaseConnection) {
		return getDataSource(databaseConnection.getJdbcUrl(), databaseConnection.getUserName(),
				new String(databaseConnection.getPassword()));
	}

	protected DataSource getDataSource(String url, String username, String password) {
		return new DriverManagerDataSource(url, username, password);
	}

}
