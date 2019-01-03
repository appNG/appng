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
package org.appng.testsupport.persistence;

import org.dbunit.ext.hsqldb.HsqldbConnection;
import org.dbunit.ext.mysql.MySqlConnection;

/**
 * Utility class that helps retrieving a {@link ConnectionInfo}.
 * 
 * @author Matthias Müller
 * 
 */
public class ConnectionHelper {

	private static final String HSQL_PORT = "hsqlPort";
	public static final String HIBERNATE_CONNECTION_URL = "hibernate.connection.url";
	private static final int MYSQL_DEFAULT_PORT = 3306;
	private static final DBType type = DBType.HSQL;

	public enum DBType {
		MYSQL, HSQL, DERBY
	}

	public static ConnectionInfo getMySqlConnectionInfo() {
		return getMySqlConnectionInfo("aitwf_testdb", "mysql-testdb", "root", "mysql");
	}

	public static ConnectionInfo getHSqlConnectionInfo(int port) {
		return getHSqlConnectionInfo("hsql-testdb", port, "hsql-testdb", "sa", "");
	}

	public static ConnectionInfo getHSqlConnectionInfo() {
		return getHSqlConnectionInfo(getHsqlPort());
	}

	public static ConnectionInfo getMySqlConnectionInfo(String database, String persistenceUnit, String user,
			String password) {
		ConnectionInfo connectionInfo = new ConnectionInfo("jdbc:mysql://localhost:" + MYSQL_DEFAULT_PORT + "/"
				+ database, MYSQL_DEFAULT_PORT, user, password, "com.mysql.jdbc.Driver", persistenceUnit,
				MySqlConnection.class);
		return connectionInfo;
	}

	public static ConnectionInfo getHSqlConnectionInfo(String database, int port, String persistenceUnit, String user,
			String password) {
		ConnectionInfo connectionInfo = new ConnectionInfo("jdbc:hsqldb:hsql://localhost:" + port + "/" + database,
				port, user, password, "org.hsqldb.jdbc.JDBCDriver", persistenceUnit, HsqldbConnection.class);
		return connectionInfo;
	}

	public static int getHsqlPort() {
		int hsqlPort = HsqlServer.DEFAULT_PORT;
		String portProperty = System.getProperty(HSQL_PORT);
		if (null == portProperty) {
			portProperty = System.getenv(HSQL_PORT);
		}
		if (null != portProperty) {
			try {
				hsqlPort = Integer.parseInt(portProperty);
			} catch (NumberFormatException e) {
				//
			}
		}
		System.setProperty(HSQL_PORT, String.valueOf(hsqlPort));
		return hsqlPort;
	}

	public static ConnectionInfo getHSqlConnectionInfo(String database, String persistenceUnit, String user,
			String password) {
		return getHSqlConnectionInfo(database, getHsqlPort(), persistenceUnit, user, password);
	}

	public static ConnectionInfo getConnectionInfo() {
		return getConnectionInfo(type);
	}

	public static ConnectionInfo getConnectionInfo(DBType type) {
		switch (type) {
		case HSQL:
			return getHSqlConnectionInfo(HsqlServer.DEFAULT_PORT);

		case MYSQL:
			return getMySqlConnectionInfo();
		}
		throw new IllegalArgumentException("no such type :" + type);
	}

}
