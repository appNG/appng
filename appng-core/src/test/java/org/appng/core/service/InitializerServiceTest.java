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
package org.appng.core.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.TestSupport;
import org.appng.core.domain.SiteImpl;
import org.appng.testapplication.TestEntity;
import org.appng.testapplication.TestService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Rollback(false)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:platformContext.xml", initializers = InitializerServiceTest.class)
@DirtiesContext
public class InitializerServiceTest extends TestSupport
		implements ApplicationContextInitializer<GenericApplicationContext> {

	public static final String TARGET_TEST_CLASSES = "target" + File.separator + "test-classes";

	@Autowired
	ConfigurableApplicationContext context;

	@Autowired
	EntityManager entityManager;

	@Autowired
	InitializerService service;

	static boolean initalized = false;

	DefaultEnvironment env;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		if (!initalized) {
			new InitTestDataProvider().writeTestData(entityManager);
			initalized = true;
		}
		env = Mockito.mock(DefaultEnvironment.class);
		Mockito.when(env.getServletContext()).thenReturn(ctx);
		Enumeration<String> emptyEnum = new Enumeration<String>() {

			public String nextElement() {
				return null;
			}

			public boolean hasMoreElements() {
				return false;
			}
		};

		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT)).thenReturn(context);
		Mockito.when(ctx.getInitParameterNames()).thenReturn(emptyEnum);
		Mockito.when(ctx.getAttributeNames()).thenReturn(emptyEnum);

		Mockito.when(ctx.getResourceAsStream(Mockito.anyString())).thenAnswer(new Answer<InputStream>() {
			public InputStream answer(InvocationOnMock invocation) throws Throwable {
				String name = (String) invocation.getArguments()[0];
				if (name.startsWith("/WEB-INF")) {
					name = TARGET_TEST_CLASSES + name;
				}
				if (name.startsWith("target/test-classes/WEB-INF")) {
					name = name.replace("target/test-classes/WEB-INF", "target/root/WEB-INF");
				}
				File file = new File(new File("").getAbsoluteFile(), name);
				return new FileInputStream(file);
			}
		});
		Mockito.when(ctx.getRealPath("/applications")).thenReturn(TARGET_TEST_CLASSES + "/applications");
		Mockito.when(ctx.getRealPath("/templates")).thenReturn("src/test/resources/template");
		platformMap.put(Platform.Environment.CORE_PLATFORM_CONTEXT, context);
	}

	@Test
	public void testInitPlatform() throws Exception {
		PropertyHolder propertyHolder = new PropertyHolder(PropertySupport.PREFIX_PLATFORM,
				Collections.<org.appng.api.model.Property> emptyList());
		for (String prop : platformProperties.getPropertyNames()) {
			String key = prop.substring(PropertySupport.PREFIX_PLATFORM.length());
			String value = platformProperties.getString(prop);
			propertyHolder.addProperty(key, value == null ? StringUtils.EMPTY : value, StringUtils.EMPTY);
		}
		propertyHolder.setFinal();
		platformMap.put(Platform.Environment.PLATFORM_CONFIG, propertyHolder);

		File templateRoot = new File("target/test-classes/repository/site-1/www/template/");
		FileUtils.deleteQuietly(templateRoot);
		Mockito.when(ctx.getRealPath("/uploads")).thenReturn("target/uploads");
		
		
		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES)).thenReturn(new HashMap<String, Site>());
		
		Mockito.when(env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG))
				.thenReturn(platformProperties);
		service.loadPlatform(new java.util.Properties(), env, null, null, null);
		Mockito.verify(ctx, Mockito.atLeastOnce()).getRealPath(Mockito.anyString());
		Mockito.verify(env,VerificationModeFactory.atLeast(1)).setAttribute(Mockito.eq(Scope.PLATFORM), Mockito.anyString(),
				Mockito.any());
	
		Assert.assertTrue(new File(templateRoot, "assets/favicon.ico").exists());
		Assert.assertTrue(new File(templateRoot, "resources/dummy.txt").exists());
		Assert.assertFalse(new File(templateRoot, "xsl").exists());
		Assert.assertFalse(new File(templateRoot, "conf").exists());
		Assert.assertFalse(new File(templateRoot, "template.xml").exists());
	}

	@Test
	@Ignore("causes CoreServiceTest to fail")
	public void testLoadSite() throws InvalidConfigurationException, IOException {
		FileUtils.copyDirectory(new File("src/test/resources/applications/application1"),
				new File("target/root/applications/application1"));
		Site siteToLoad = siteMap.remove("manager");
		service.loadSite((SiteImpl) siteToLoad, ctx, new FieldProcessorImpl("testLoadSite"));

		Application application = siteToLoad.getApplication("application1");
		TestService testservice = application.getBean(TestService.class);
		TestEntity entity = new TestEntity(null, "name", 2, 3.4d, true);
		testservice.createEntity(entity);
		Assert.assertEquals(Integer.valueOf(1), entity.getId());
		service.shutDownSite(env, siteToLoad);
	}

	@Override
	public void initialize(GenericApplicationContext applicationContext) {
		new TestInitializer() {

			@Override
			protected java.util.Properties getProperties() {
				java.util.Properties properties = super.getProperties();
				properties.put("entityPackage", "org.appng.testapplication");
				properties.put("hsqlPort", "9010");
				properties.put("hsqlPath", "file:target/hsql/" + InitializerServiceTest.class.getSimpleName());
				properties.put("repositoryBase", "org.appng.testapplication");
				return properties;
			}
		}.initialize(applicationContext);
	}

}
