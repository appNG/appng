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
package org.appng.core.service;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.testsupport.persistence.ConnectionHelper;
import org.hsqldb.Server;
import org.junit.Assert;
import org.junit.Test;

public class HsqlStarterTest {

	@Test(timeout = 20000)
	public void testStartStop() throws Exception {
		Properties platformProperties = new Properties();
		int port = ConnectionHelper.getHsqlPort();
		platformProperties.put("database.port", String.valueOf(port));
		platformProperties.put(DatabaseService.DATABASE_TYPE, DatabaseType.HSQL.name());

		Server server = HsqlStarter.startHsql(platformProperties, "target/appNG");
		String jdbcUrl = "jdbc:hsqldb:hsql://localhost:" + port + "/appng";
		try {
			DriverManager.getConnection(jdbcUrl, "sa", "");
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}

		List<String> hsqlThreads = getHsqlThreads();
		HsqlStarter.shutdown(server);

		try {
			DriverManager.getConnection(jdbcUrl, "sa", "");
			Assert.fail("getConnection() should fail");
		} catch (SQLException e) {
		}

		while (hsqlThreads.size() > 0) {
			Thread.sleep(1000);
			hsqlThreads = getHsqlThreads();
		}

		Assert.assertTrue("HSQL Threads should be empty, but there are " + hsqlThreads.size(), hsqlThreads.isEmpty());
	}

	private List<String> getHsqlThreads() {
		return Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().startsWith("HSQLDB"))
				.map(Thread::getName).collect(Collectors.toList());
	}

}
