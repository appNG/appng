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
package org.appng.appngizer.controller;

import org.appng.appngizer.model.xml.Group;
import org.appng.appngizer.model.xml.Groups;
import org.appng.appngizer.model.xml.Subject;
import org.appng.appngizer.model.xml.UserType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SubjectControllerTest extends ControllerTest {

	@Test
	public void testCreateRetrieveAndUpdate() throws Exception {
		Group group = new Group();
		group.setName("Admin");
		postAndVerify("/group", null, group, HttpStatus.CREATED);

		Subject subject = new Subject();
		subject.setName("Admin");
		subject.setDescription("the admin");
		subject.setRealName("Admin Istrator");
		subject.setEmail("admin@appng.org");
		subject.setTimeZone("Europe/London");
		subject.setLanguage("en");
		subject.setDigest("$2a$13$4r/t9xYlTLbrOYZy67My3eE9/QacQgSqRFCgvhbzi7TQAE5AYptpO");
		subject.setType(UserType.LOCAL_USER);
		subject.setGroups(new Groups());
		subject.getGroups().getGroup().add(group);

		postAndVerify("/subject", "xml/subject-create.xml", subject, HttpStatus.CREATED);
		postAndVerify("/subject", null, subject, HttpStatus.CONFLICT);

		subject.setDescription("a subject for administrators");
		subject.setEmail("admin@appng.com");
		subject.setTimeZone("Europe/Madrid");
		subject.setLanguage("es");
		subject.getGroups().getGroup().clear();
		putAndVerify("/subject/Admin", "xml/subject-update.xml", subject, HttpStatus.OK);

	}

	@Test
	public void testDelete() throws Exception {
		Subject created = new Subject();
		created.setName("deleteme");
		created.setDescription("deleteme");
		created.setRealName("deleteme");
		created.setEmail("deleteme@appng.org");
		created.setTimeZone("Europe/Paris");
		created.setLanguage("fr");
		created.setType(UserType.GLOBAL_USER);

		postAndVerify("/subject", null, created, HttpStatus.CREATED);
		deleteAndVerify("/subject/deleteme", "", HttpStatus.NO_CONTENT);
	}

	@Test
	public void testList() throws Exception {
		getAndVerify("/subject", "xml/subject-list.xml", HttpStatus.OK);
	}
}