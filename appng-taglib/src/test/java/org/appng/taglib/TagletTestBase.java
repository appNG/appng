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
package org.appng.taglib;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.jsp.PageContext;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

public abstract class TagletTestBase {

	protected static PageContext setupTagletTest(Map<String, String> parameters) {
		MockServletContext servletContext = new MockServletContext();

		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		Map<String, Object> requestScope = new ConcurrentHashMap<>();
		requestScope.put("variable", "fromRequest");
		requestScope.put(EnvironmentKeys.SERVLETPATH, "/en/page/fromUrl");
		request.setAttribute(Scope.REQUEST.name(), requestScope);

		if (null != parameters) {
			request.addParameters(parameters);
		}

		Map<String, Object> sessionScope = new ConcurrentHashMap<>();
		request.setSession(new MockHttpSession(servletContext));
		request.getSession().setAttribute(Scope.SESSION.name(), sessionScope);
		sessionScope.put("variable", "fromSession");

		Properties properties = Mockito.mock(Properties.class);
		Mockito.when(properties.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		Map<String, Object> platformScope = new ConcurrentHashMap<>();
		platformScope.put(Platform.Environment.PLATFORM_CONFIG, properties);
		Map<String, Site> siteMap = new HashMap<>();
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getName()).thenReturn("localhost");
		Mockito.when(site.getHost()).thenReturn("localhost");
		Mockito.when(site.getDomain()).thenReturn("localhost");
		Mockito.when(site.getProperties()).thenReturn(Mockito.mock(Properties.class));
		siteMap.put("localhost", site);
		platformScope.put(Platform.Environment.SITES, siteMap);
		platformScope.put("variable", "fromPlatform");

		servletContext.setAttribute(Scope.PLATFORM.name(), platformScope);
		Map<String, Object> siteScope = new ConcurrentHashMap<>();
		siteScope.put("variable", "fromSite");
		servletContext.setAttribute(Scope.SITE.name() + ".localhost", siteScope);
		return new MockPageContext(servletContext, request);
	}
}
