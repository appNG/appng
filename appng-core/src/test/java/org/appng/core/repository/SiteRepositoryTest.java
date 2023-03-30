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
package org.appng.core.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.Sets;

@ContextConfiguration(initializers = SiteRepositoryTest.class)
public class SiteRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	SiteRepository repository;

	@Autowired
	ApplicationRepository applicationRepository;

	@Autowired
	SiteApplicationRepository siteApplicationRepository;

	public void test() {
		Set<String> aliases = Sets.newHashSet("host-a");

		SiteImpl site = new SiteImpl();
		site.setName("name");
		site.setHost("host");
		site.setDomain("domain");
		site.setHostAliases(aliases);
		repository.save(site);

		SiteImpl site2 = new SiteImpl();
		site2.setName("name2");
		site2.setHost("host-a");
		site2.setDomain("domain2");
		repository.save(site2);

		Assert.assertEquals(2, repository.findSitesForHostNames(aliases).size());

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
		Assert.assertEquals(Arrays.asList(1, 2), repository.getSiteIds());

		List<SiteImpl> siteList = Arrays.asList(site, site2);
		Assert.assertEquals(siteList, repository.findSites());

		List<SiteImpl> singleSite = Arrays.asList(site);
		Assert.assertEquals(singleSite, repository.findSitesForApplication(application.getId()));
		Assert.assertEquals(singleSite, repository.findSitesForApplication(application.getId(), false));
		site.setActive(true);
		Assert.assertEquals(singleSite, repository.findSitesForApplication(application.getId(), true));
	}

}
