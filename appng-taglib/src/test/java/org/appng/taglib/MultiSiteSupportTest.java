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
package org.appng.taglib;

import static org.appng.api.Scope.PLATFORM;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.CoreService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

public class MultiSiteSupportTest {

	@Test
	public void test() {
		MultiSiteSupport multiSiteSupport = new MultiSiteSupport();
		String siteName = "localhost";
		String applicationName = "application";

		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setSession(new MockHttpSession(servletContext));
		MockPageContext pageContext = new MockPageContext(servletContext, request);

		Properties properties = Mockito.mock(Properties.class);
		Mockito.when(properties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		Map<String, Object> platformScope = new ConcurrentHashMap<>();
		platformScope.put(Platform.Environment.PLATFORM_CONFIG, properties);

		Site site = Mockito.mock(SiteImpl.class);
		Mockito.when(site.getName()).thenReturn(siteName);
		Mockito.when(site.getHost()).thenReturn(siteName);
		Mockito.when(site.getDomain()).thenReturn(siteName);
		Mockito.when(site.getProperties()).thenReturn(Mockito.mock(Properties.class));
		Map<String, Site> siteMap = new HashMap<>();
		siteMap.put(siteName, site);
		platformScope.put(Platform.Environment.SITES, siteMap);

		servletContext.setAttribute(Scope.PLATFORM.name(), platformScope);

		DefaultEnvironment environment = DefaultEnvironment.get(pageContext);
		ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
		environment.setAttribute(PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT, ctx);
		CoreService coreService = Mockito.mock(CoreService.class);
		Mockito.when(ctx.getBean(CoreService.class)).thenReturn(coreService);

		Mockito.when(coreService.getGrantingSite(siteName, applicationName)).thenReturn(null);
		try {
			multiSiteSupport.process(environment, applicationName, "method", request);
			Assert.fail("must throw exception");
		} catch (Exception e) {
			Assert.assertEquals("no application '" + applicationName + "' for site '" + siteName + "'", e.getMessage());
		}
	}
}
