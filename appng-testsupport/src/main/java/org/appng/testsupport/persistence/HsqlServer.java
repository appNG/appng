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
package org.appng.testsupport.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.hsqldb.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO insert description
 * 
 * @author mueller.matthias, aiticon GmbH, 2011
 * 
 */
public class HsqlServer {

	public static final int DEFAULT_PORT = 9001;

	private static final String HSQL_TESTDB = "hsql-testdb";

	private static final Logger LOG = LoggerFactory.getLogger(HsqlServer.class);

	private static volatile int startCount = 0;

	public static void main(String[] args) {
		HsqlServer.start(DEFAULT_PORT);
		HsqlServer.stop(DEFAULT_PORT);
		HsqlServer.start(DEFAULT_PORT);
		HsqlServer.stop(DEFAULT_PORT);
	}

	public static void start() {
		start(HSQL_TESTDB, DEFAULT_PORT);
	}

	public static void start(String dbName) {
		start(dbName, DEFAULT_PORT);
	}

	public static void stop() {
		stop(HSQL_TESTDB, DEFAULT_PORT);
	}

	public static void stop(String dbName) {
		stop(dbName, DEFAULT_PORT);
	}

	public static void start(int port) {
		start(HSQL_TESTDB, port);
	}

	public static void start(String dbName, int port) {
		start(dbName, dbName, port);
	}

	public static void start(String dbName, String folder, int port) {
		if (startCount == 0) {
			LOG.debug("############# (" + startCount + ") starting HsqlServer");
			String[] params = new String[] { "-database.0", "file:target/hsql/" + folder + "/", "-dbname.0", dbName,
					"-no_system_exit", "true", "-port", String.valueOf(port) };
			Server.main(params);
		} else {
			LOG.debug("############# (" + startCount + ") HsqlServer already started");
		}
		startCount++;
	}

	public static void stop(int port) {
		stop(HSQL_TESTDB, port);
	}

	public static void stop(String dbName, int port) {
		startCount--;
		if (0 == startCount) {
			LOG.debug("############# (" + startCount + ") shutting down HsqlServer");
			shutdown(dbName, port);
		} else {
			LOG.debug("############# (" + startCount + ") not shutting down HsqlServer");
		}
	}

	private static void shutdown(String dbName, int port) {
		try {
			String jdbcUrl = "jdbc:hsqldb:hsql://localhost:" + port + "/" + dbName;
			Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
			Statement createStatement = connection.createStatement();
			createStatement.execute("SHUTDOWN");
		} catch (SQLException e) {
			LOG.info("failed shutting down, must be first start:" + e.getMessage());
		}
	}

}
