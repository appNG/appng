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
package org.appng.core.repository.config;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.sla.jdbcperflogger.driver.WrappingDriver;

/**
 * 
 * A {@link DatasourceConfigurer} using <a href="http://brettwooldridge.github.io/HikariCP/">HikariCP</a>. Also supports
 * <a href="https://github.com/sylvainlaurent/JDBC-Performance-Logger">JDBC-Performance-Logger</a> for measuring
 * performance of SQL statements.
 * 
 * @author Matthias Müller
 */
public class HikariCPConfigurer implements DatasourceConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(HikariCPConfigurer.class);
	private HikariDataSource hikariDataSource;
	private boolean logPerformance = false;

	public HikariCPConfigurer() {

	}

	public HikariCPConfigurer(DatabaseConnection connection) {
		configure(connection);
	}

	public HikariCPConfigurer(DatabaseConnection connection, boolean logPerformance) {
		this.logPerformance = logPerformance;
		configure(connection);
	}

	public void configure(DatabaseConnection connection) {
		HikariConfig configuration = new HikariConfig();

		configuration.setMaximumPoolSize(connection.getMaxConnections());
		if (StringUtils.isNotBlank(connection.getValidationQuery())) {
			configuration.setConnectionTestQuery(connection.getValidationQuery());
		}
		configuration.setPoolName(connection.getName());

		DatabaseType type = connection.getType();
		configuration.setRegisterMbeans(true);
		String jdbcUrl = connection.getJdbcUrl();
		boolean isPerfLoggerUrl = jdbcUrl.startsWith(WrappingDriver.URL_PREFIX);
		if (logPerformance || isPerfLoggerUrl) {
			String realJdbcUrl = isPerfLoggerUrl ? jdbcUrl : WrappingDriver.URL_PREFIX + jdbcUrl;
			configuration.setJdbcUrl(realJdbcUrl);
			configuration.setUsername(connection.getUserName());
			configuration.setPassword(connection.getPasswordPlain());
			configuration.setDriverClassName(WrappingDriver.class.getName());
			LOGGER.info("connection {} uses driver {}", realJdbcUrl, WrappingDriver.class.getName());
		} else {
			String dataSourceClassName = type.getDataSourceClassName();
			String urlProperty = "url";
			if (DatabaseType.MSSQL.equals(type)) {
				urlProperty = "URL";
			}
			configuration.setDataSourceClassName(dataSourceClassName);
			configuration.addDataSourceProperty(urlProperty, jdbcUrl);
			configuration.addDataSourceProperty("user", connection.getUserName());
			configuration.addDataSourceProperty("password", connection.getPasswordPlain());
			LOGGER.info("connection {} uses datasource {}", jdbcUrl, dataSourceClassName);
		}
		this.hikariDataSource = new HikariDataSource(configuration);
	}

	public void destroy() {
		hikariDataSource.close();
		hikariDataSource = null;
	}

	public DataSource getDataSource() {
		return hikariDataSource;
	}

	public boolean isLogPerformance() {
		return logPerformance;
	}

	public void setLogPerformance(boolean logPerformance) {
		this.logPerformance = logPerformance;
	}

}
