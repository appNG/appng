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
package org.appng.core.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.appng.api.model.Group;
import org.appng.api.model.Role;
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;

public class InitTestDataProvider extends AppNGTestDataProvider {

	@Override
	public void writeTestData(EntityManager em) {

		SubjectImpl subject1 = getSubject(3, UserType.LOCAL_USER, new ArrayList<Group>());
		em.persist(subject1);
		Set<Subject> subjects = new HashSet<Subject>();
		subjects.add(subject1);

		SiteImpl site1 = getSite(1);
		em.persist(site1);

		ApplicationImpl application1 = getApplication("application1", em);
		em.persist(application1);

		RoleImpl applicationRole1 = getApplicationRole(1, application1, "dummy");
		PermissionImpl permission1 = getPermission(1, application1);
		em.persist(applicationRole1);
		em.persist(permission1);

		application1.getPermissions().add(permission1);
		application1.getRoles().add(applicationRole1);

		site1.getApplications().add(application1);

		Set<Role> roles = new HashSet<Role>();
		roles.add(applicationRole1);

		GroupImpl group1 = getGroup(1, roles, subjects);
		em.persist(group1);

		applicationRole1.getPermissions().add(permission1);
		subject1.getGroups().add(group1);
	}
}
