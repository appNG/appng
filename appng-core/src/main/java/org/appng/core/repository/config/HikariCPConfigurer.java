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
package org.appng.core.repository.config;

import org.apache.commons.lang3.StringUtils;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.sla.jdbcperflogger.driver.WrappingDriver;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link DatasourceConfigurer} using <a href="http://brettwooldridge.github.io/HikariCP/">HikariCP</a>. Also supports
 * <a href="https://github.com/sylvainlaurent/JDBC-Performance-Logger">JDBC-Performance-Logger</a> for measuring
 * performance of SQL statements.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class HikariCPConfigurer implements DatasourceConfigurer {

	private @Getter HikariDataSource dataSource;
	private @Setter boolean logPerformance = false;
	private @Setter long connectionTimeout = DEFAULT_TIMEOUT;
	private @Setter long validationTimeout = DEFAULT_TIMEOUT;
	private @Setter long maxLifetime = DEFAULT_LIFE_TIME;
	private @Setter boolean autoCommit = false;

	public HikariCPConfigurer() {

	}

	public void configure(DatabaseConnection connection) {
		HikariConfig configuration = new HikariConfig();

		configuration.setMinimumIdle(connection.getMinConnections());
		configuration.setMaximumPoolSize(connection.getMaxConnections());
		configuration.setConnectionTimeout(connectionTimeout);
		configuration.setValidationTimeout(validationTimeout);
		configuration.setMaxLifetime(maxLifetime);
		if (StringUtils.isNotBlank(connection.getValidationQuery())) {
			configuration.setConnectionTestQuery(connection.getValidationQuery());
		}
		configuration.setPoolName(connection.getName());
		configuration.setAutoCommit(autoCommit);

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
		this.dataSource = new HikariDataSource(configuration);
	}

	public void destroy() {
		dataSource.close();
		dataSource = null;
	}

}
