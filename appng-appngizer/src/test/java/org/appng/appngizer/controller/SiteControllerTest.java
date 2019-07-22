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

import org.appng.appngizer.model.xml.Site;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SiteControllerTest extends ControllerTest {

	@Test
	public void testCreateRetrieveAndUpdate() throws Exception {
		Site created = new Site();
		created.setName("localhost");
		created.setHost("localhost");
		created.setDomain("http://localhost:8081");
		created.setDescription("none");
		created.setActive(false);
		created.setCreateRepositoryPath(true);

		postAndVerify("/site", "xml/site-create.xml", created, HttpStatus.CREATED);
		postAndVerify("/site", null, created, HttpStatus.CONFLICT);

		Site updated = new Site();
		updated.setName("localhost");
		updated.setHost("localhost");
		updated.setDomain("http://localhost:8080");
		updated.setDescription("the local host");
		updated.setActive(true);
		putAndVerify("/site/localhost", "xml/site-update.xml", updated, HttpStatus.OK);

	}

	@Test
	public void testDelete() throws Exception {
		Site created = new Site();
		created.setName("deleteme");
		created.setHost("deleteme");
		created.setDomain("http://deleteme:8080");
		created.setDescription("deleteme");
		created.setActive(false);

		postAndVerify("/site", null, created, HttpStatus.CREATED);
		deleteAndVerify("/site/deleteme", "", HttpStatus.NO_CONTENT);
	}

	@Test
	public void testList() throws Exception {
		getAndVerify("/site", "xml/site-list.xml", HttpStatus.OK);
	}
}