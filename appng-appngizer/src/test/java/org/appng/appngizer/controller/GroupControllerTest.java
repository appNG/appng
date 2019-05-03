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

import org.appng.appngizer.model.xml.Group;
import org.appng.appngizer.model.xml.Role;
import org.appng.appngizer.model.xml.Roles;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GroupControllerTest extends ControllerTest {

	@Test
	public void testCreateRetrieveAndUpdate() throws Exception {
		installApplication();

		Group created = new Group();
		created.setName("Admin");
		created.setDescription("a group for admins");
		created.setRoles(new Roles());
		Role r = new Role();
		r.setApplication("demo-application");
		r.setName("Administrator");
		created.getRoles().getRole().add(r);

		postAndVerify("/group", "xml/group-create.xml", created, HttpStatus.CREATED);
		postAndVerify("/group", null, created, HttpStatus.CONFLICT);

		Group updated = new Group();
		updated.setName("Administrator");
		updated.setDescription("a group for administrators");
		MockHttpServletResponse response = putAndVerify("/group/Admin", null, updated, HttpStatus.SEE_OTHER);
		assertLocation("http://localhost/group/Administrator", response);

		getAndVerify("/group/Administrator", "xml/group-update.xml", HttpStatus.OK);
	}

	@Test
	public void testDelete() throws Exception {
		Group created = new Group();
		created.setName("deleteme");
		created.setDescription("deleteme");

		postAndVerify("/group", null, created, HttpStatus.CREATED);
		deleteAndVerify("/group/deleteme", "", HttpStatus.NO_CONTENT);
	}

	@Test
	public void testList() throws Exception {
		getAndVerify("/group", "xml/group-list.xml", HttpStatus.OK);
	}
}