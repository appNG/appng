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
package org.appng.core.repository.config;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.appng.core.JMXUtils;
import org.appng.core.domain.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DatasourceConfigurer} based on the <a href="http://tomcat.apache.org/tomcat-7.0-doc/jdbc-pool.html">Tomcat
 * JDBC Connection Pool</a>.
 * 
 * @author Matthias Müller
 * 
 */
public class TomcatJdbcConfigurer implements DatasourceConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(TomcatJdbcConfigurer.class);
	private org.apache.tomcat.jdbc.pool.DataSource tomcatDatasource;

	public TomcatJdbcConfigurer() {

	}

	public TomcatJdbcConfigurer(DatabaseConnection connection) {
		configure(connection);
	}

	public void configure(DatabaseConnection connection) {
		// see
		// http://tomcat.apache.org/tomcat-8.5-doc/api/index.html?org/apache/tomcat/jdbc/pool/DataSource.html
		this.tomcatDatasource = new org.apache.tomcat.jdbc.pool.DataSource();
		tomcatDatasource.setUrl(connection.getJdbcUrl());
		tomcatDatasource.setUsername(connection.getUserName());
		tomcatDatasource.setPassword(new String(connection.getPassword()));
		tomcatDatasource.setDriverClassName(connection.getDriverClass());
		tomcatDatasource.setName(connection.getName());
		tomcatDatasource.setInitialSize(connection.getMinConnections());
		tomcatDatasource.setMaxActive(connection.getMaxConnections());
		tomcatDatasource.setValidationInterval(connection.getValidationPeriod() * 60 * 1000);
		tomcatDatasource.setValidationQuery(connection.getValidationQuery());
		if (tomcatDatasource.getMaxIdle() > tomcatDatasource.getMaxActive()) {
			tomcatDatasource.setMaxIdle(tomcatDatasource.getMaxActive());
		}
		try {
			ConnectionPool pool = tomcatDatasource.createPool();
			JMXUtils.register(pool.getJmxPool(), JMX_DOMAIN + ":type=" + tomcatDatasource.getName());
		} catch (Exception e) {
			LOGGER.error("error while creating pool " + this, e);
		}
	}

	public void destroy() {
		tomcatDatasource.close();
		LOGGER.info("TomcatJdbcConfigurer#" + hashCode() + " about to destroy " + tomcatDatasource);
		JMXUtils.unregister(JMX_DOMAIN + ":type=" + tomcatDatasource.getName());
	}

	public DataSource getDataSource() {
		return tomcatDatasource;
	}

	public void setLogPerformance(boolean logPerformance) {
		// not supported
	}

}
