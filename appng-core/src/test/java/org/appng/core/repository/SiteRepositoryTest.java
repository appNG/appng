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
package org.appng.core.repository;

import java.util.Arrays;
import java.util.List;

import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = SiteRepositoryTest.class)
public class SiteRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	SiteRepository repository;

	@Autowired
	ApplicationRepository applicationRepository;

	@Autowired
	SiteApplicationRepository siteApplicationRepository;

	public void test() {
		SiteImpl site = new SiteImpl();
		site.setName("name");
		site.setHost("host");
		site.setDomain("domain");
		repository.save(site);

		ApplicationImpl application = new ApplicationImpl();
		application.setName("name");
		applicationRepository.save(application);

		SiteApplication siteApplication = new SiteApplication(site, application);
		site.getSiteApplications().add(siteApplication);
		siteApplicationRepository.save(siteApplication);

		Assert.assertEquals(site, repository.findByDomain(site.getDomain()));
		Assert.assertEquals(site, repository.findByHost(site.getHost()));
		Assert.assertEquals(site, repository.findByName(site.getName()));
		Assert.assertEquals(site, repository.findOne(site.getId()));
		Assert.assertEquals(Arrays.asList(1), repository.getSiteIds());

		List<SiteImpl> siteList = Arrays.asList(site);
		Assert.assertEquals(siteList, repository.findSites());

		Assert.assertEquals(siteList, repository.findSitesForApplication(application.getId()));
		Assert.assertEquals(siteList, repository.findSitesForApplication(application.getId(), false));
		site.setActive(true);
		Assert.assertEquals(siteList, repository.findSitesForApplication(application.getId(), true));
	}

}
