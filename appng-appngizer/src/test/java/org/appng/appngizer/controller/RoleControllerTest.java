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
package org.appng.appngizer.controller;

import org.appng.appngizer.model.xml.Permission;
import org.appng.appngizer.model.xml.Permissions;
import org.appng.appngizer.model.xml.Role;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoleControllerTest extends ControllerTest {

	@Test
	public void testDotInName() throws Exception {
		Role role = new Role();
		role.setName("with.dot");
		role.setApplication("demo-application");
		postAndVerify("/application/demo-application/role", "", role, HttpStatus.CREATED);
		putAndVerify("/application/demo-application/role/with.dot", "", role, HttpStatus.OK);
		deleteAndVerify("/application/demo-application/role/with.dot", "", HttpStatus.NO_CONTENT);
	}

	@Test
	public void testInvalidName() throws Exception {
		Role role = new Role();
		role.setName("john doe!");
		role.setApplication("demo-application");
		postAndVerify("/application/demo-application/role", "", role, HttpStatus.BAD_REQUEST);
	}

	@Test
	public void testCreateRetrieveAndUpdate() throws Exception {
		installApplication();
		getAndVerify("/application/demo-application/role", "xml/role-list.xml", HttpStatus.OK);

		getAndVerify("/application/demo-application/role/Administrator", "xml/role-show.xml", HttpStatus.OK);

		Role role = new Role();
		role.setApplication("demo-application");
		role.setName("Editor");
		role.setDescription("editor role");
		role.setPermissions(new Permissions());
		Permission p1 = new Permission();
		p1.setName("output-type.webgui");
		p1.setApplication("demo-application");
		role.getPermissions().getPermission().add(p1);
		Permission p2 = new Permission();
		p2.setName("output-format.html");
		p2.setApplication("demo-application");
		role.getPermissions().getPermission().add(p2);
		postAndVerify("/application/demo-application/role", "xml/role-create.xml", role, HttpStatus.CREATED);
		postAndVerify("/application/demo-application/role", null, role, HttpStatus.CONFLICT);

		role.setDescription("The editor role");
		role.getPermissions().getPermission().remove(1);
		putAndVerify("/application/demo-application/role/Editor", "xml/role-update.xml", role, HttpStatus.OK);
	}

}
