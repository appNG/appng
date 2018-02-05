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
package org.appng.appngizer.controller;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.appng.appngizer.model.xml.Database;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class DatabaseControllerTest extends ControllerTest {

	@Autowired
	DataSource datasource;

	@Test
	public void test() throws Exception {
		ignorePasswordAndInstalledDate();
		getAndVerify("/platform/database", "xml/database.xml", HttpStatus.OK);
	}

	@Test
	public void testInitialize() throws Exception {
		ignorePasswordAndInstalledDate();
		postAndVerify("/platform/database/initialize", "xml/database-init.xml", null, HttpStatus.OK);

		testUpdateRoot();
	}

	public void testUpdateRoot() throws Exception {
		String sql = IOUtils.resourceToString("init-db.sql", StandardCharsets.UTF_8, getClass().getClassLoader());
		datasource.getConnection().prepareStatement(sql).execute();
		ignorePasswordAndInstalledDate();
		Database database = new Database();
		database.setType(DatabaseType.HSQL.name());
		database.setManaged(true);
		database.setUser("");
		database.setPassword("");
		database.setDbVersion("");
		database.setDriver("");
		database.setUrl("");
		putAndVerify("/platform/database", "xml/database-root-update.xml", database, HttpStatus.OK);
	}

	private void ignorePasswordAndInstalledDate() {
		differenceListener.ignoreDifference("/database/password/text()");
		differenceListener.ignoreDifference("/database/versions/version/@installed");
	}
}
