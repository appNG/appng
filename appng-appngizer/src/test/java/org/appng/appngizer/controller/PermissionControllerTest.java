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
package org.appng.appngizer.controller;

import javax.servlet.http.HttpServletResponse;

import org.appng.appngizer.model.xml.Permission;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class PermissionControllerTest extends ControllerTest {

	@Test
	public void test() throws Exception {
		installApplication();
		getAndVerify("/application/demo-application/permission", "xml/permission-list.xml", HttpStatus.OK);

		getAndVerify("/application/demo-application/permission/testPermission", "xml/permission-show.xml",
				HttpStatus.OK);
		Permission p = new Permission();
		p.setName("testPermission");
		p.setDescription("foo, bar!");

		putAndVerify("/application/demo-application/permission/testPermission", "xml/permission-update.xml", p,
				HttpStatus.OK);
		p.setName("newName");
		HttpServletResponse response = putAndVerify("/application/demo-application/permission/testPermission", null, p,
				HttpStatus.SEE_OTHER);
		assertLocation("http://localhost/application/demo-application/permission/newName", response);

		deleteAndVerify("/application/demo-application", null, HttpStatus.NO_CONTENT);
	}
}
