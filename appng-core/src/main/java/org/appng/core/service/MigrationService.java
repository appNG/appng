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
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * A service offering methods for configuring and initializing {@link DatabaseConnection}s using
 * <a href="http://www.flywaydb.org">Flyway</a>.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class MigrationService {

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

	private static final String LOCATION_PREFIX = "db/migration/";

	/**
	 * Enum type defining the different states of a database migration.
	 * 
	 * @author Matthias Müller
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
		/** database not migrated */
		DB_NOT_MIGRATED,
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
	 *               the properties read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 * 
	 * @return the appNG root {@link DatabaseConnection}
	 */
	public DatabaseConnection initDatabase(java.util.Properties config) {
		DatabaseConnection platformConnection = getPlatformConnection(config);
		Boolean doRepair = Boolean.valueOf(config.getProperty(DATABASE_REPAIR));
		initDatabase(platformConnection, doRepair);
		return platformConnection;
	}

	public DatabaseConnection getPlatformConnection(java.util.Properties properties) {
		String type = properties.getProperty(DATABASE_TYPE);
		DatabaseType databaseType = DatabaseType.MYSQL;
		if (StringUtils.isEmpty(type)) {
			LOGGER.warn("Property {} is not specified, using default: {}", DATABASE_TYPE, databaseType.name());
		} else {
			try {
				databaseType = DatabaseType.valueOf(type.toUpperCase());
			} catch (IllegalArgumentException e) {
				LOGGER.error("Invalid value for property {}: {} ; must be one of: {}", DATABASE_TYPE, type,
						Arrays.asList(DatabaseType.values()));
			}
		}

		String jdbcUrl = properties.getProperty(HIBERNATE_CONNECTION_URL);
		String driverClass = properties.getProperty(HIBERNATE_CONNECTION_DRIVER_CLASS);
		String username = properties.getProperty(HIBERNATE_CONNECTION_USERNAME);
		String password = properties.getProperty(HIBERNATE_CONNECTION_PASSWORD);

		Integer minConnections = Integer.valueOf((String) properties.getOrDefault(DATABASE_MIN_CONNECTIONS, "5"));
		Integer maxConnections = Integer.valueOf((String) properties.getOrDefault(DATABASE_MAX_CONNECTIONS, "20"));
		String validationQuery = properties.getProperty(DATABASE_VALIDATION_QUERY);
		String validationPeriod = properties.getProperty(DATABASE_VALIDATION_PERIOD);

		DatabaseConnection conn = new DatabaseConnection(databaseType, jdbcUrl, driverClass, username,
				password.getBytes(), validationQuery);
		conn.setName(DATABASE_NAME_PREFIX + databaseType.name());
		conn.setDescription(APP_NG_ROOT_DATABASE);
		if (StringUtils.isNotBlank(validationPeriod)) {
			conn.setValidationPeriod(Integer.valueOf(validationPeriod));
		}
		conn.registerDriver(true);
		conn.setMigrationInfoService(statusComplete(conn));
		conn.setMinConnections(minConnections);
		conn.setMaxConnections(maxConnections);
		return conn;
	}

	/**
	 * Returns the current {@link MigrationInfo} for the connection
	 * 
	 * @param config
	 *               the configuration read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 * 
	 * @return the current {@link MigrationInfo} for the given connection
	 * 
	 * @see #status(DatabaseConnection)
	 */
	public MigrationInfo status(java.util.Properties config) {
		return status(getPlatformConnection(config));
	}

	/**
	 * Returns the current {@link MigrationInfo} for the given {@link DatabaseConnection}
	 * 
	 * @param connection
	 *                   a {@link DatabaseConnection}
	 * 
	 * @return the current {@link MigrationInfo} for the given connection (may be {@code null}).
	 * 
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
	 *                   a {@link DatabaseConnection}
	 * 
	 * @return the current {@link MigrationInfoService} for the given connection (may be {@code null}).
	 * 
	 * @see MigrationInfoService
	 */
	public MigrationInfoService statusComplete(DatabaseConnection connection) {
		return statusComplete(connection, true);
	}

	/**
	 * Returns the current {@link MigrationInfoService} for the given {@link DatabaseConnection} (the appNG root
	 * connection).
	 * 
	 * @param connection
	 *                       a {@link DatabaseConnection}
	 * @param testConnection
	 *                       if the connection needs to be tested
	 * 
	 * @return the current {@link MigrationInfoService} for the given connection (may be {@code null}).
	 * 
	 * @see MigrationInfoService
	 */
	public MigrationInfoService statusComplete(DatabaseConnection connection, boolean testConnection) {
		StringBuilder dbInfo = new StringBuilder();
		if (!testConnection || connection.testConnection(dbInfo, true)) {
			LOGGER.info("connected to {} ({})", connection.getJdbcUrl(), dbInfo.toString());
			Flyway flyway = getFlyway(connection, null, LOCATION_PREFIX + connection.getType().name().toLowerCase());
			MigrationInfoService info = flyway.info();
			connection.setMigrationInfoService(info);
			return info;
		} else {
			LOGGER.error("{} is not working, unable to retrieve connection status.", connection.toString());
		}
		return null;
	}

	/**
	 * Returns the current {@link MigrationInfoService} for the given {@link DatabaseConnection}, which must be owned by
	 * a {@link SiteApplication}.
	 * 
	 * @param connection
	 *                   a {@link DatabaseConnection} owned by a {@link SiteApplication}
	 * @param sqlFolder
	 *                   the path to migration scripts
	 * 
	 * @return the current {@link MigrationInfoService} for the given connection (may be {@code null}).
	 * 
	 * @see MigrationInfoService
	 */
	public MigrationInfoService statusComplete(DatabaseConnection connection, File sqlFolder) {
		if (null != connection && connection.testConnection(null)) {
			String typeFolder = connection.getType().name().toLowerCase();
			File scriptFolder = new File(sqlFolder.getAbsolutePath(), typeFolder);
			Flyway flyway = getFlyway(connection, null, Location.FILESYSTEM_PREFIX + scriptFolder.getAbsolutePath());
			MigrationInfoService info = flyway.info();
			connection.setMigrationInfoService(info);
			return info;
		}
		return null;
	}

	protected Flyway getFlyway(DatabaseConnection connection, ClassLoader classLoader, String... locations) {
		// Flyway changed the name of the table from "schema_version" to
		// "flyway_schema_history"
		// https://github.com/flyway/flyway/issues/1965
		FluentConfiguration configuration = null == classLoader ? new FluentConfiguration()
				: new FluentConfiguration(classLoader);
		return configuration.table("schema_version").dataSource(connection.getDataSource()).locations(locations).load();
	}

	protected MigrationStatus initDatabase(DatabaseConnection rootConnection, Boolean doRepair) {
		StringBuilder dbInfo = new StringBuilder();
		String jdbcUrl = rootConnection.getJdbcUrl();
		if (rootConnection.testConnection(dbInfo, true)) {
			LOGGER.info("connected to {} ({})", jdbcUrl, dbInfo.toString());
			Flyway flyway = getFlyway(rootConnection, null,
					LOCATION_PREFIX + rootConnection.getType().name().toLowerCase());
			if (doRepair) {
				flyway.repair();
			}
			return migrate(flyway, rootConnection);
		} else {
			LOGGER.error("{} is not working, initializing database was not successful.", rootConnection.toString());
		}
		return MigrationStatus.ERROR;
	}

	protected MigrationStatus migrate(Flyway flyway, DatabaseConnection databaseConnection) {
		try {
			flyway.migrate();
			return MigrationStatus.DB_MIGRATED;
		} catch (FlywayException e) {
			LOGGER.error(String.format("error while migrating %s", databaseConnection.getJdbcUrl()), e);
		}
		return MigrationStatus.ERROR;
	}

	protected DataSource getDataSource(DatabaseConnection databaseConnection) {
		return databaseConnection.getDataSource();
	}

	protected DataSource getDataSource(String url, String username, String password) {
		return new DriverManagerDataSource(url, username, password);
	}

}
