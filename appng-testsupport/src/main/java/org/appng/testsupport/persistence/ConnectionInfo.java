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
package org.appng.testsupport.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.Driver;
import java.util.List;

import javax.sql.DataSource;

import org.dbunit.database.IDatabaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * A container for all information about a database connection like the JDBC-Url, user, password etc.
 * 
 * @author Matthias Müller
 * 
 */
public class ConnectionInfo {

	private String jdbcUrl;

	private int port;

	private String user;

	private String password;

	private String driverClass;

	private String persistenceUnit;

	private Class<? extends IDatabaseConnection> connection;

	private List<String> tableNames;

	private Driver driver;

	public ConnectionInfo(String jdbcUrl, int port, String user, String password, String driverClass,
			String persistenceUnit, Class<? extends IDatabaseConnection> connection) {
		super();
		this.jdbcUrl = jdbcUrl;
		this.port = port;
		this.user = user;
		this.password = password;
		this.driverClass = driverClass;
		this.persistenceUnit = persistenceUnit;
		this.connection = connection;

		try {
			driver = (Driver) Class.forName(driverClass).newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("can not instantiate driver class", e);
		}
	}

	/**
	 * @return the jdbcUrl
	 */
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	/**
	 * @param jdbcUrl
	 *            the jdbcUrl to set
	 */
	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the driverClass
	 */
	public String getDriverClass() {
		return driverClass;
	}

	/**
	 * @param driverClass
	 *            the driverClass to set
	 */
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	/**
	 * @return the connection
	 */
	public Class<? extends IDatabaseConnection> getConnection() {
		return connection;
	}

	/**
	 * @param connection
	 *            the connection to set
	 */
	public void setConnection(Class<? extends IDatabaseConnection> connection) {
		this.connection = connection;
	}

	/**
	 * @return the persistenceUnit
	 */
	public String getPersistenceUnit() {
		return persistenceUnit;
	}

	/**
	 * @param persistenceUnit
	 *            the persistenceUnit to set
	 */
	public void setPersistenceUnit(String persistenceUnit) {
		this.persistenceUnit = persistenceUnit;
	}

	/**
	 * @return the tableNames
	 */
	public List<String> getTableNames() {
		return tableNames;
	}

	/**
	 * @param tableNames
	 *            the tableNames to set
	 */
	public void setTableNames(List<String> tableNames) {
		this.tableNames = tableNames;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void executeSqlFromResource(String resourceName) throws IOException {
		InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
		String viewScript = ScriptUtils.readScript(new LineNumberReader(new InputStreamReader(resource)),
				ScriptUtils.DEFAULT_COMMENT_PREFIX, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
		executeSql(viewScript);
	}

	public void executeSql(String sql) {
		DataSource dataSource = new SimpleDriverDataSource(driver, getJdbcUrl(), getUser(), getPassword());
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute(sql);
	}

}
