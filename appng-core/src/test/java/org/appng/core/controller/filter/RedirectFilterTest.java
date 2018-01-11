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
package org.appng.core.controller.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.controller.filter.RedirectFilter.UrlRewriteConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.tuckey.web.filters.urlrewrite.NormalRule;
import org.tuckey.web.filters.urlrewrite.Rule;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;

public class RedirectFilterTest {

	@Test
	public void test() throws IOException, ServletException {
		ResourceLoader resourceLoader = new ResourceLoader() {
			public Resource getResource(String location) {
				return new ClassPathResource("conf/urlrewrite.xml");
			}

			public ClassLoader getClassLoader() {
				return null;
			}
		};

		Properties props = Mockito.mock(Properties.class);
		Mockito.when(props.getString(Platform.Property.VHOST_MODE)).thenReturn(VHostMode.NAME_BASED.name());
		Map<String, Site> sites = new HashMap<String, Site>();
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getHost()).thenReturn("localhost");
		Mockito.when(site.getName()).thenReturn("localhost");
		Mockito.when(site.getDomain()).thenReturn("localhost");
		Mockito.when(site.getProperties()).thenReturn(props);
		sites.put(site.getHost(), site);

		ConcurrentMap<Object, Object> platform = new ConcurrentHashMap<>();
		platform.put(Platform.Environment.PLATFORM_CONFIG, props);
		platform.put(Platform.Property.TIME_ZONE, TimeZone.getDefault().getDisplayName());
		platform.put(Platform.Property.LOCALE, Locale.GERMANY.getDisplayName());
		platform.put(Platform.Environment.SITES, sites);
		Mockito.when(props.getString(Platform.Property.JSP_FILE_TYPE)).thenReturn(".jsp");

		MockServletContext servletContext = new MockServletContext(resourceLoader);
		servletContext.setAttribute(Scope.PLATFORM.name(), platform);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		MockFilterChain chain = new MockFilterChain();
		MockFilterConfig filterConfig = new MockFilterConfig(servletContext);
		filterConfig.addInitParameter("confReloadCheckInterval", "5000");

		RedirectFilter redirectFilter = new RedirectFilter();
		redirectFilter.init(filterConfig);
		redirectFilter.doFilter(request, response, chain);
		UrlRewriter urlRewriter = redirectFilter.getUrlRewriter(request, response, chain);
		UrlRewriteConfig conf = (UrlRewriteConfig) urlRewriter.getConf();
		verifyRule(conf.getRules().get(0), "^/app$", "http://foobar.org");
		verifyRule(conf.getRules().get(1), "^/en/page.jsp$", "/de/seite");
		verifyRule(conf.getRules().get(2), "^/en/page.jsp/(.*)$", "/de/seite/${encode:utf8:$1}");
		verifyRule(conf.getRules().get(3), "^/de/index$", "/en/index.jsp");
		verifyRule(conf.getRules().get(4), "^/en/index.jsp$", "/de/index");
		verifyRule(conf.getRules().get(5), "^/de/error$", "/de/fehler.jsp");
		verifyRule(conf.getRules().get(6), "^/fr/accueil$", "/fr/index.jsp");
		verifyRule(conf.getRules().get(7), "^/fr/index.jsp$", "/fr/accueil");
		verifyRule(conf.getRules().get(8), "^/de/fault((\\?\\S+)?)$", "/de/fehler.jsp$1");

	}

	private void verifyRule(Rule rule, String from, String to) {
		Assert.assertEquals(from, ((NormalRule) rule).getFrom());
		Assert.assertEquals(to, ((NormalRule) rule).getTo());
	}
}
