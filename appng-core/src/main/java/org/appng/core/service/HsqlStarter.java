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
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.hsqldb.Server;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class responsible for starting and stopping a HSQL {@link Server} in case appNG is configured to use
 * {@link DatabaseType#HSQL}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class HsqlStarter {

	private static final String APPNG_HSQL_LOG = "appng-hsql.log";
	private static final String APPNG_HSQL_ERROR_LOG = "appng-hsql-error.log";
	private static final String DATABASE_NAME = "appng";
	private static final String DATABASE_PORT = "database.port";
	private static final String FOLDER_DATABASE = "WEB-INF/database";

	public static final String CONTEXT = "hsqlContext";

	/**
	 * Checks if the database-type is hsql and if {@value #DATABASE_PORT} is set.
	 */
	public static boolean mustStartServer(Properties platformProperties) {
		return DatabaseType.HSQL.name().equalsIgnoreCase(platformProperties.getProperty(DatabaseService.DATABASE_TYPE))
				&& StringUtils.isNotBlank(platformProperties.getProperty(DATABASE_PORT));
	}

	/**
	 * Starts a HSQL {@link Server}, but only if {@link DatabaseType#HSQL} is the configured type and
	 * {@value #DATABASE_PORT} is set.
	 * 
	 * @param  platformProperties
	 *                            the properties read from
	 *                            {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 * @param  appngHome
	 *                            the home folder of appNG
	 * 
	 * @return                    a {@link Server}-instance, if {@link DatabaseType#HSQL} is the configured type.
	 * 
	 * @throws IOException
	 *                            in case the database folder or hsql logfiles could not be accessed
	 */
	public static Server startHsql(Properties platformProperties, String appngHome) throws IOException {
		if (mustStartServer(platformProperties)) {
			File databaseRootPath = new File(appngHome, FOLDER_DATABASE);
			FileUtils.forceMkdir(databaseRootPath);

			File databasePath = new File(databaseRootPath, DATABASE_NAME);
			String port = platformProperties.getProperty(DATABASE_PORT);

			Server server = new Server();
			server.setLogWriter(null);
			server.setSilent(true);
			server.setLogWriter(new PrintWriter(new File(databaseRootPath, APPNG_HSQL_LOG)));
			server.setErrWriter(new PrintWriter(new File(databaseRootPath, APPNG_HSQL_ERROR_LOG)));
			server.setTrace(false);
			server.setPort(Integer.valueOf(port));
			server.setDatabaseName(0, DATABASE_NAME);
			server.setDatabasePath(0, databasePath.getAbsolutePath());
			server.setNoSystemExit(true);

			LOGGER.info("starting HSQL Server {} at {} on port {}", server.getProductVersion(),
					databasePath.getAbsolutePath(), port);
			server.start();
			return server;
		} else {
			LOGGER.debug("not running on HSQL");
		}
		return null;
	}

	/**
	 * Shuts down the given {@link Server}, if non-null.
	 * 
	 * @param server
	 *               the {@link Server} to shut down, may be {@code null}
	 */
	public static void shutdown(Server server) {
		if (null != server) {
			LOGGER.info("shutting down HSQL Server {} at {} on port {}", server.getProductVersion(),
					server.getDatabasePath(0, false), server.getPort());
			String jdbcUrl = String.format("jdbc:hsqldb:hsql://localhost:%s/%s", server.getPort(), DATABASE_NAME);
			try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
					Statement statement = connection.createStatement()) {
				statement.execute("SHUTDOWN");
			} catch (SQLException e) {
				LOGGER.warn("error while shutting down server", e);
			}
			server.shutdown();
		} else {
			LOGGER.debug("not running on HSQL, nothing to shutdown");
		}
	}

}
