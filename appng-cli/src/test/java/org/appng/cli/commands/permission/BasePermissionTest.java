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
package org.appng.cli.commands.permission;

import java.util.Arrays;
import java.util.List;

import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.repository.ApplicationRepository;
import org.appng.core.repository.PermissionRepository;
import org.appng.core.repository.RoleRepository;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BasePermissionTest extends AbstractCommandTest {

	protected static final String ROLE = "admin";
	protected static final String APPLICATION = "appng-manager";
	protected static final List<String> PERMISSIONS = Arrays.asList("debug", "foobar");

	@Autowired
	protected ApplicationRepository applicationRepository;

	@Autowired
	protected RoleRepository applicationRoleRepository;

	@Autowired
	protected PermissionRepository permissionRepository;

	@Override
	@Before
	public void setup() {
		super.setup();
		ApplicationImpl application = new ApplicationImpl();
		application.setName(APPLICATION);
		application.setApplicationVersion("1.0.0");
		application.setPrivileged(true);
		applicationRepository.save(application);

		RoleImpl role = new RoleImpl();
		role.setName("admin");
		role.setApplication(application);
		applicationRoleRepository.save(role);

		PermissionImpl p1 = new PermissionImpl();
		p1.setApplication(application);
		p1.setName("debug");
		permissionRepository.save(p1);

		PermissionImpl p2 = new PermissionImpl();
		p2.setApplication(application);
		p2.setName("foobar");
		permissionRepository.save(p2);
	}
}
