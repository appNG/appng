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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.hsqldb.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class HsqlServerFactoryBean implements FactoryBean<Server>, InitializingBean, DisposableBean {

	private static final Logger LOG = LoggerFactory.getLogger(HsqlServerFactoryBean.class);

	private String databaseName = "hsql-testdb";
	private int port = 9001;
	private String databasePath;
	private Server server;

	public Server getObject() throws Exception {
		return server;
	}

	public Class<?> getObjectType() {
		return Server.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void destroy() {
		LOG.debug("shutting down HSQL Server {} at {} on port {}", server.getProductVersion(),
				server.getDatabasePath(0, false), server.getPort());
		String jdbcUrl = String.format("jdbc:hsqldb:hsql://localhost:%s/%s", server.getPort(), databaseName);
		try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
			connection.createStatement().execute("SHUTDOWN");
		} catch (SQLException e) {
			LOG.warn("error while shutting down server", e);
		}
		server.shutdown();
	}

	public void init() {
		LOG.debug("starting HSQL Server {} at {} on port {}", server.getProductVersion(),
				server.getDatabasePath(0, false), server.getPort());
		server.start();
	}

	public void afterPropertiesSet() throws Exception {
		server = new Server();
		server.setLogWriter(null);
		server.setSilent(true);
		server.setNoSystemExit(true);
		server.setDatabaseName(0, getDatabaseName());
		server.setDatabasePath(0, getDatabasePath());
		server.setPort(getPort());
		server.setTrace(false);
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDatabasePath() {
		return null == databasePath ? ("file:target/hsql/" + databaseName) : databasePath;
	}

	public void setDatabasePath(String databasePath) {
		if (null != databasePath) {
			this.databasePath = databasePath;
		}
	}

	@Override
	public String toString() {
		return "database " + getDatabaseName() + " at port " + port + " (" + getDatabasePath() + ")";
	}

}
