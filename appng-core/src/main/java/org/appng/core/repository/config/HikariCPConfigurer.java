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
package org.appng.core.repository.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 
 * A {@link DatasourceConfigurer} using <a href="http://brettwooldridge.github.io/HikariCP/">HikariCP</a>.
 * 
 * @author Matthias Müller
 */
public class HikariCPConfigurer implements DatasourceConfigurer {

	private HikariDataSource hikariDataSource;

	public HikariCPConfigurer() {

	}

	public HikariCPConfigurer(DatabaseConnection connection) {
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
		String dataSourceClassName = type.getDataSourceClassName();
		String urlProperty = "url";
		if (DatabaseType.MSSQL.equals(type)) {
			urlProperty = "URL";
		}

		configuration.setDataSourceClassName(dataSourceClassName);
		configuration.setRegisterMbeans(true);
		Properties dsProperties = new Properties();
		dsProperties.put(urlProperty, connection.getJdbcUrl());
		dsProperties.put("user", connection.getUserName());
		dsProperties.put("password", connection.getPasswordPlain());
		configuration.setDataSourceProperties(dsProperties);

		this.hikariDataSource = new HikariDataSource(configuration);

	}

	public void destroy() {
		hikariDataSource.close();
		hikariDataSource = null;
	}

	public DataSource getDataSource() {
		return hikariDataSource;
	}

}
