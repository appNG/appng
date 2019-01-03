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
package org.appng.appngizer.controller;

import javax.sql.DataSource;

import org.appng.appngizer.model.xml.Database;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatabaseControllerTest extends ControllerTest {

	@Autowired
	DataSource datasource;

	@Test
	public void testInitialize() throws Exception {
		ignorePasswordAndInstalledDate();
		postAndVerify("/platform/database/initialize", "xml/database-init.xml", null, HttpStatus.OK);
	}

	@Test
	public void testInitialized() throws Exception {
		ignorePasswordAndInstalledDate();
		getAndVerify("/platform/database", "xml/database-init.xml", HttpStatus.OK);
	}

	@Test
	public void testInitializeManaged() throws Exception {
		ignorePasswordAndInstalledDate();
		postAndVerify("/platform/database/initialize?managed=true", "xml/database-init-managed.xml", null,
				HttpStatus.OK);

	}

	@Test
	public void testUpdateRoot() throws Exception {
		ignorePasswordAndInstalledDate();
		Database database = new Database();
		database.setType(DatabaseType.HSQL.name());
		database.setManaged(false);
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
