/*
 * Copyright 2011-2021 the original author or authors.
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
package org.appng.core.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.appng.api.Environment;
import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.model.Group;
import org.appng.api.model.Subject;
import org.appng.api.support.DefaultPermissionProcessor;
import org.appng.api.support.RequestSupportImpl;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldPermissions;
import org.appng.xml.platform.Permission;
import org.appng.xml.platform.PermissionMode;
import org.appng.xml.platform.Permissions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PermissionProcessorTest {
	@Mock
	private Subject subject;

	@Mock
	private Environment env;

	@Mock
	private org.appng.forms.Request formRequest;

	private ApplicationImpl application;

	private SiteImpl site;

	private PermissionProcessor processor;

	private PermissionProcessor noSubjectProcessor;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		RequestSupportImpl requestSupport = new RequestSupportImpl();
		requestSupport.setEnvironment(env);

		application = new ApplicationImpl();
		application.setName("application");
		site = new SiteImpl();
		site.setName("site");

		List<Group> groups = new ArrayList<>();
		GroupImpl group = new GroupImpl();
		group.setName("admingroup");
		groups.add(group);

		Mockito.when(subject.getGroups()).thenReturn(groups);
		RoleImpl applicationRole = new RoleImpl();
		applicationRole.setName("application-admin");
		applicationRole.setApplication(application);
		group.getRoles().add(applicationRole);

		Set<org.appng.api.model.Permission> permissions = applicationRole.getPermissions();
		org.appng.api.model.Permission p1 = getPermission("permission.A");
		permissions.add(p1);
		org.appng.api.model.Permission p3 = getPermission("some.permission");
		permissions.add(p3);
		org.appng.api.model.Permission p2 = getPermission("permission.B");
		permissions.add(p2);

		Mockito.when(subject.getName()).thenReturn("admin");
		processor = new DefaultPermissionProcessor(subject, site, application);
		noSubjectProcessor = new DefaultPermissionProcessor(new SubjectImpl(), site, application);
	}

	private org.appng.api.model.Permission getPermission(String name) {
		PermissionImpl p = new PermissionImpl();
		p.setName(name);
		p.setApplication(application);
		return p;
	}

	@Test
	public void testWrite() {
		FieldDef fieldDefinition = new FieldDef();
		Assert.assertTrue(processor.hasWritePermission(fieldDefinition));
		Assert.assertTrue(noSubjectProcessor.hasWritePermission(fieldDefinition));

		Condition condition = new Condition();
		condition.setExpression("${1 == 1}");
		fieldDefinition.setCondition(condition);
		Assert.assertTrue(processor.hasWritePermission(fieldDefinition));
		Assert.assertTrue(noSubjectProcessor.hasWritePermission(fieldDefinition));

		condition.setExpression("${1 == 2}");
		Assert.assertTrue(processor.hasWritePermission(fieldDefinition));
		Assert.assertTrue(noSubjectProcessor.hasWritePermission(fieldDefinition));
		fieldDefinition.setCondition(null);

		fieldDefinition.setReadonly(Boolean.TRUE.toString());
		Assert.assertFalse(processor.hasWritePermission(fieldDefinition));
		Assert.assertFalse(noSubjectProcessor.hasWritePermission(fieldDefinition));

		Permissions permissions = getPermissions();
		FieldPermissions fieldPermissions = new FieldPermissions();
		fieldPermissions.getPermission().addAll(permissions.getPermissionList());
		fieldDefinition.getPermissions().add(fieldPermissions);

		Assert.assertFalse(processor.hasWritePermission(fieldDefinition));
		Assert.assertFalse(noSubjectProcessor.hasWritePermission(fieldDefinition));
		fieldDefinition.setReadonly(Boolean.FALSE.toString());
		Assert.assertTrue(processor.hasWritePermission(fieldDefinition));
		Assert.assertFalse(noSubjectProcessor.hasWritePermission(fieldDefinition));

	}

	@Test
	public void testRead() {
		FieldDef fieldDefinition = new FieldDef();
		Assert.assertTrue(processor.hasReadPermission(fieldDefinition));
		Assert.assertTrue(noSubjectProcessor.hasReadPermission(fieldDefinition));

		fieldDefinition.setReadonly(Boolean.TRUE.toString());
		Assert.assertTrue(processor.hasReadPermission(fieldDefinition));
		Assert.assertTrue(noSubjectProcessor.hasReadPermission(fieldDefinition));

		Permissions permissions = getPermissions();
		FieldPermissions fieldPermissions = new FieldPermissions();
		fieldPermissions.getPermission().addAll(permissions.getPermissionList());
		fieldDefinition.getPermissions().add(fieldPermissions);

		Assert.assertTrue(processor.hasReadPermission(fieldDefinition));
		Assert.assertFalse(noSubjectProcessor.hasReadPermission(fieldDefinition));
		fieldDefinition.setReadonly(Boolean.FALSE.toString());
		Assert.assertTrue(processor.hasReadPermission(fieldDefinition));
		Assert.assertFalse(noSubjectProcessor.hasReadPermission(fieldDefinition));
	}

	@Test
	public void testAnonymous() {
		Config config = new Config();
		Permissions permissions = new Permissions();
		config.setPermissions(permissions);
		Permission anon1 = new Permission();
		anon1.setRef("anonymous.a");
		permissions.getPermissionList().add(anon1);
		Permission anon2 = new Permission();
		anon2.setRef("anonymous2");
		permissions.getPermissionList().add(anon2);
		Assert.assertTrue(processor.hasPermissions(new PermissionOwner(config)));
	}

	private Permissions getPermissions() {
		Permissions permissions = new Permissions();

		Permission permission = new Permission();
		permission.setRef("some.permission");
		permission.setMode(PermissionMode.SET);
		permissions.getPermissionList().add(permission);

		Permission noMode = new Permission();
		noMode.setRef("another.permission");
		permissions.getPermissionList().add(noMode);

		return permissions;
	}
}
