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
package org.appng.testsupport;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@ContextConfiguration(locations = { TestBase.TESTCONTEXT }, inheritLocations = false)
public class TestBaseTest extends TestBase {

	@Override
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		initEnvironment();
		application = new SimpleApplication("dummy", context, getApplicationSubjects());
	}

	@Override
	protected Properties getProperties() {
		return new Properties();
	}

	@Test
	public void testGetHandlerMethodArgumentResolver() throws Exception {
		HandlerMethodArgumentResolver hmar = getHandlerMethodArgumentResolver();
		Method method = TestBaseTest.class.getMethod("setApplication", Environment.class, Site.class, Application.class,
				String.class);
		MethodParameter envParameter = MethodParameter.forMethodOrConstructor(method, 0);
		MethodParameter siteParameter = MethodParameter.forMethodOrConstructor(method, 1);
		MethodParameter appParameter = MethodParameter.forMethodOrConstructor(method, 2);
		MethodParameter notSupported = MethodParameter.forMethodOrConstructor(method, 3);

		Assert.assertTrue(hmar.supportsParameter(envParameter));
		Assert.assertTrue(hmar.supportsParameter(siteParameter));
		Assert.assertTrue(hmar.supportsParameter(appParameter));
		Assert.assertFalse(hmar.supportsParameter(notSupported));

		Assert.assertEquals(environment, hmar.resolveArgument(envParameter, null, null, null));
		Assert.assertEquals(site, hmar.resolveArgument(siteParameter, null, null, null));
		Assert.assertEquals(application, hmar.resolveArgument(appParameter, null, null, null));
		Assert.assertNull(hmar.resolveArgument(notSupported, null, null, null));

	}

	@Test
	public void testInitEnvironment() {
		org.appng.api.model.Properties platformCfg = environment.getAttribute(Scope.PLATFORM,
				Platform.Environment.PLATFORM_CONFIG);
		Assert.assertFalse(platformCfg.getPlainProperties().isEmpty());
		Assert.assertNotNull(platformCfg);

		Map<String, Site> siteMap = environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		Assert.assertNotNull(siteMap);
		Assert.assertEquals(site, siteMap.get("localhost"));
		Assert.assertFalse(site.getProperties().getPlainProperties().isEmpty());

	}

	public void setApplication(Environment environment, Site site, Application application, String foo) {
		// used in testGetHandlerMethodArgumentResolver()
	}

}
