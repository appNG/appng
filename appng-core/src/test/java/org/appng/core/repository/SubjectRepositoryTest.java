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
package org.appng.core.repository;

import java.util.Arrays;

import org.appng.api.model.UserType;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = SubjectRepositoryTest.class)
public class SubjectRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	SubjectRepository repository;

	@Autowired
	ApplicationRepository applicationRepository;

	@Autowired
	RoleRepository applicationRoleRepository;

	@Autowired
	SiteRepository siteRepository;

	@Autowired
	SiteApplicationRepository siteApplicationRepository;

	@Autowired
	GroupRepository groupRepository;

	public void test() {

		GroupImpl group = new GroupImpl();
		group.setName("name");
		groupRepository.save(group);

		SiteImpl site = new SiteImpl();
		site.setName("name");
		site.setHost("host");
		site.setDomain("domain");
		siteRepository.save(site);

		ApplicationImpl application = new ApplicationImpl();
		application.setName("name");
		applicationRepository.save(application);

		SiteApplication siteApplication = new SiteApplication(site, application);
		site.getSiteApplications().add(siteApplication);
		siteApplicationRepository.save(siteApplication);

		RoleImpl applicationRole = new RoleImpl();
		applicationRole.setApplication(application);
		applicationRole.setName("name");
		group.getRoles().add(applicationRole);
		applicationRoleRepository.save(applicationRole);

		SubjectImpl subject = new SubjectImpl();
		subject.setName("name");
		subject.setRealname("John Doe");
		subject.setLanguage("de");
		subject.setEmail("example@foo.org");
		subject.setUserType(UserType.LOCAL_USER);
		subject.getGroups().add(group);
		repository.save(subject);

		Assert.assertEquals(subject, repository.findByName(subject.getName()));
		Assert.assertEquals(subject, repository.findByEmail(subject.getEmail()));
		Assert.assertEquals(Arrays.asList(subject), repository.findByUserType(subject.getUserType()));
		Assert.assertEquals(subject, repository.findById(subject.getId()).get());

		Assert.assertEquals(Arrays.asList(subject), repository.findSubjectsForApplication(application.getId()));

		repository.delete(subject);
		Assert.assertFalse(repository.findById(subject.getId()).isPresent());
	}
}
