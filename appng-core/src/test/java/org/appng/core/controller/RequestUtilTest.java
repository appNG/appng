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
package org.appng.core.controller;

import static org.appng.api.Scope.PLATFORM;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.model.Property;
import org.appng.api.model.Site;
import org.appng.api.support.PropertyHolder;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.PropertySupport;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RequestUtilTest {

	@Mock
	private Environment environment;

	@Mock
	private ServletRequest servletRequest;

	@Test
	public void testGetSite() {
		MockitoAnnotations.initMocks(this);
		PropertyHolder propertyHolder = new PropertyHolder(PropertySupport.PREFIX_PLATFORM, new ArrayList<Property>());
		new PropertySupport(propertyHolder).initPlatformConfig("target/root", true);
		Mockito.when(environment.getAttribute(PLATFORM, Platform.Environment.PLATFORM_CONFIG)).thenReturn(
				propertyHolder);

		Mockito.when(servletRequest.getServerName()).thenReturn("host-2");
		Map<String, Site> sites = new HashMap<String, Site>();
		sites.put("site-1", getSite(1));
		sites.put("site-2", getSite(2));
		Mockito.when(environment.getAttribute(PLATFORM, Platform.Environment.SITES)).thenReturn(sites);
		Site site2 = RequestUtil.getSite(environment, servletRequest);
		Assert.assertEquals("site-2", site2.getName());
		Assert.assertEquals("host-2", site2.getHost());

		Site siteByHost = RequestUtil.getSiteByHost(environment, "host-2");
		Assert.assertEquals("site-2", siteByHost.getName());
		Assert.assertEquals("host-2", siteByHost.getHost());

		Site siteBySite = RequestUtil.getSiteByName(environment, "site-2");
		Assert.assertEquals("site-2", siteBySite.getName());
		Assert.assertEquals("host-2", siteBySite.getHost());

	}

	private SiteImpl getSite(int i) {
		SiteImpl site = new SiteImpl();
		site.setActive(true);
		site.setDescription("Site description-" + i);
		site.setHost("host-" + i);
		site.setName("site-" + i);
		site.setDomain("http://www.localhost.de:808" + i);
		site.setVersion(new Date());
		return site;
	}
}
