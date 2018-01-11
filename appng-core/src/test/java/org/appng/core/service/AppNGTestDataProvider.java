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
package org.appng.core.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.appng.api.model.Application;
import org.appng.api.model.Group;
import org.appng.api.model.Permission;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Role;
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.domain.ResourceImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.appng.testsupport.persistence.TestDataProvider;

public class AppNGTestDataProvider implements TestDataProvider {

	public static final String SALT = "vh/ehxDEkAM=";
	public static final String DIGEST = "VlBQQcXL+lpSZwu86CSYmdaB3pY=";

	public void writeTestData(EntityManager em) {
		SubjectImpl subject1 = getSubject(1, UserType.GLOBAL_GROUP, new ArrayList<Group>());
		SubjectImpl subject2 = getSubject(2, UserType.GLOBAL_USER, new ArrayList<Group>());
		SubjectImpl subject3 = getSubject(3, UserType.LOCAL_USER, new ArrayList<Group>());
		subject3.setDigest(DIGEST);
		subject3.setSalt(SALT);

		em.persist(subject1);
		em.persist(subject2);
		em.persist(subject3);

		Set<Subject> subjects = new HashSet<Subject>();
		subjects.add(subject1);
		subjects.add(subject2);
		subjects.add(subject3);

		SiteImpl site1 = getSite(1);
		SiteImpl site2 = getSite(2);
		SiteImpl site3 = getSite(3);

		em.persist(site1);
		em.persist(site2);
		em.persist(site3);

		ApplicationImpl application1 = getApplication("manager", em);
		ApplicationImpl application2 = getApplication("bugtracker", em);
		ApplicationImpl application3 = getApplication("foobar", em);

		em.persist(application1);
		em.persist(application2);
		em.persist(application3);

		String propertyName1 = PropertySupport.getPropertyName(site1, application1, "foobar");
		PropertyImpl p1 = new PropertyImpl(propertyName1, "foobaz");
		em.persist(p1);

		String propertyName2 = PropertySupport.getPropertyName(null, application3, "foobar");
		PropertyImpl p2 = new PropertyImpl(propertyName2, "foobaz");
		em.persist(p2);

		ResourceImpl applicationResource1 = getApplicationResource(1);
		ResourceImpl applicationResource2 = getApplicationResource(2);
		ResourceImpl applicationResource3 = getApplicationResource(3);

		applicationResource1.setApplication(application1);
		applicationResource2.setApplication(application2);
		applicationResource3.setApplication(application3);

		em.persist(applicationResource1);
		em.persist(applicationResource2);
		em.persist(applicationResource3);

		RoleImpl applicationRole1 = getApplicationRole(1, application1, "admin");
		RoleImpl applicationRole2 = getApplicationRole(2, application2, "user");
		RoleImpl applicationRole3 = getApplicationRole(3, application3, "debugger");

		PermissionImpl permission1 = getPermission(1, application1);
		PermissionImpl permission2 = getPermission(2, application2);
		PermissionImpl permission3 = getPermission(3, application3);

		em.persist(applicationRole1);
		em.persist(applicationRole2);
		em.persist(applicationRole3);

		em.persist(permission1);
		em.persist(permission2);
		em.persist(permission3);

		application1.getPermissions().add(permission1);
		application2.getPermissions().add(permission2);
		application3.getPermissions().add(permission3);

		application1.getRoles().add(applicationRole1);
		application2.getRoles().add(applicationRole2);
		application3.getRoles().add(applicationRole3);

		SiteApplication siteApplication1 = new SiteApplication(site1, application1);
		em.persist(siteApplication1);
		site1.getSiteApplications().add(siteApplication1);
		SiteApplication siteApplication2 = new SiteApplication(site2, application2);
		em.persist(siteApplication2);
		site2.getSiteApplications().add(siteApplication2);
		SiteApplication siteApplication3 = new SiteApplication(site3, application3);
		em.persist(siteApplication3);
		site3.getSiteApplications().add(siteApplication3);

		Set<Role> applicationRoles = new HashSet<Role>();
		applicationRoles.add(applicationRole1);
		applicationRoles.add(applicationRole2);
		applicationRoles.add(applicationRole3);

		GroupImpl group1 = getGroup(1, applicationRoles, subjects);
		GroupImpl group2 = getGroup(2, applicationRoles, subjects);
		GroupImpl group3 = getGroup(3, applicationRoles, subjects);

		em.persist(group1);
		em.persist(group2);
		em.persist(group3);

		applicationRole1.getPermissions().add(permission1);
		applicationRole2.getPermissions().add(permission2);
		applicationRole3.getPermissions().add(permission3);

		subject1.getGroups().add(group1);
		subject2.getGroups().add(group2);
		subject3.getGroups().add(group3);

		RepositoryImpl applicationRepository = new RepositoryImpl();
		applicationRepository.setActive(true);
		applicationRepository.setDescription("description");
		applicationRepository.setName("repo");
		applicationRepository.setPublished(true);
		applicationRepository.setRepositoryMode(RepositoryMode.ALL);
		applicationRepository.setRepositoryType(RepositoryType.LOCAL);
		try {
			applicationRepository.setUri(new URI("file:/temp/repo"));
		} catch (URISyntaxException e) {
			// ignore
		}
		em.persist(applicationRepository);
	}

	PropertyImpl getProperty(String name, String vlaue, boolean isMandatory) {
		PropertyImpl p = new PropertyImpl();
		p.setName(name);
		p.setMandatory(isMandatory);
		p.setString(vlaue);
		p.setVersion(new Date());
		return p;
	}

	PermissionImpl getPermission(int i, ApplicationImpl application) {
		PermissionImpl p = new PermissionImpl();
		p.setDescription("Permission description-" + i);
		p.setName("permission-" + i);
		p.setApplication(application);
		p.setVersion(new Date());

		return p;
	}

	ApplicationImpl getApplication(String name, EntityManager em) {
		ApplicationImpl application = new ApplicationImpl();
		application.setPrivileged(false);
		application.setDescription("description");
		application.setFileBased(false);
		application.setName(name);
		application.setPermissions(new HashSet<Permission>());
		application.setRoles(new HashSet<Role>());

		ResourceImpl applicationResource = new ResourceImpl();
		applicationResource.setResourceType(ResourceType.APPLICATION);
		applicationResource.setBytes("<application />".getBytes());
		applicationResource.setName(ResourceType.APPLICATION_XML_NAME);
		applicationResource.setApplication(application);
		applicationResource.calculateChecksum();
		em.persist(applicationResource);
		application.getResourceSet().add(applicationResource);
		return application;
	}

	RoleImpl getApplicationRole(int i, Application application, String name) {
		RoleImpl pRole = new RoleImpl();
		pRole.setDescription("ApplicationRole description-" + i);
		pRole.setName(name);
		pRole.setPermissions(new HashSet<Permission>());
		pRole.setApplication(application);
		pRole.setVersion(new Date());
		return pRole;
	}

	SiteImpl getSite(int i) {
		SiteImpl site = new SiteImpl();
		site.setActive(true);
		site.setDescription("Site description-" + i);
		site.setHost("host-" + i);
		site.setName("site-" + i);
		site.setDomain("http://www.localhost.de:808" + i);
		site.setVersion(new Date());
		return site;
	}

	GroupImpl getGroup(int i, Set<Role> applicationRoles, Set<Subject> subjects) {
		GroupImpl group = new GroupImpl();
		group.setDescription("Group description-" + i);
		group.setName("group-" + i);
		group.setVersion(new Date());
		group.setRoles(applicationRoles);
		group.setSubjects(subjects);
		return group;
	}

	SubjectImpl getSubject(int i, UserType userType, List<Group> groups) {
		SubjectImpl subject = new SubjectImpl();
		subject.setAuthenticated(true);
		subject.setDescription("Subject description-" + i);
		subject.setLanguage("DE");
		subject.setName("subject-" + i);
		subject.setEmail("subject" + i + "@aiticon.de");
		subject.setRealname("subject_username-" + i);
		subject.setUserType(userType);
		subject.setVersion(new Date());
		subject.setGroups(groups);
		return subject;

	}

	ResourceImpl getApplicationResource(int i) {
		ResourceImpl pRD = new ResourceImpl();
		pRD.setDescription("ApplicationResourceDatabased-Description-" + i);
		pRD.setName("ApplicationResourceDatabased-Name-" + i);
		pRD.setResourceType(ResourceType.ASSET);
		pRD.setBytes("".getBytes());
		return pRD;

	}
}
