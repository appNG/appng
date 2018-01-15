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
package org.appng.core.repository;

import java.util.Arrays;
import java.util.List;

import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = RoleRepositoryTest.class)
public class RoleRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	ApplicationRepository applicationRepository;

	@Autowired
	RoleRepository applicationRoleRepository;

	@Autowired
	SiteRepository siteRepository;

	@Autowired
	SiteApplicationRepository siteApplicationRepository;

	public void test() {
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
		applicationRoleRepository.save(applicationRole);

		Integer applicationId = application.getId();
		Assert.assertEquals(applicationRole,
				applicationRoleRepository.findByApplicationIdAndName(applicationId, "name"));
		List<RoleImpl> roleList = Arrays.asList(applicationRole);
		Assert.assertEquals(roleList, applicationRoleRepository.findByApplicationId(applicationId));
		Assert.assertEquals(applicationRole, applicationRoleRepository.findOne(applicationRole.getId()));
		Assert.assertEquals(roleList, applicationRoleRepository.findRolesForSite(site.getId()));
		Assert.assertEquals(roleList,
				applicationRoleRepository.findRolesForApplicationAndSite(site.getId(), applicationId));

		applicationRoleRepository.delete(applicationRole);
		Assert.assertNull(applicationRoleRepository.findOne(applicationRole.getId()));
	}
}
