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
package org.appng.core.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Request;
import org.appng.api.model.Permission;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.testapplication.TestEntity;
import org.appng.testapplication.TestService;
import org.appng.testsupport.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { ApplicationContextTest.BEANS_XML, TestBase.TESTCONTEXT,
		TestBase.TESTCONTEXT_JPA }, initializers = ApplicationContextTest.class, inheritLocations = false)
public class ApplicationContextTest extends TestBase {

	protected static final String BEANS_XML = "classpath:applications/application1/beans.xml";
	private String foo = "bar";
	private String siteFoo = "site.bar";
	private String platformFoo = "platform.bar";

	@Override
	protected Properties getProperties() {
		Properties properties = super.getProperties();
		properties.put("foo", foo);
		properties.put("site.foo", siteFoo);
		properties.put("platform.foo", platformFoo);
		return properties;
	}

	public ApplicationContextTest() {
		super("demo-application", "src/test/resources/applications/application1");
		setEntityPackage("org.appng.testapplication");
		setRepositoryBase("");
	}

	@Test
	public void test() throws InvalidConfigurationException {
		@SuppressWarnings("unchecked")
		Map<String, String> config = (Map<String, String>) application.getBean("config");
		Assert.assertEquals(foo, config.get("foo"));
		Assert.assertEquals(siteFoo, config.get("siteFoo"));
		Assert.assertEquals(platformFoo, config.get("platformFoo"));
		Assert.assertNotNull(application.getBean("marshallService"));
		Assert.assertNotNull(application.getBean("environment", Environment.class));
		MessageSource messageSource = application.getBean("messageSource", MessageSource.class);
		Assert.assertNotNull(messageSource);

		String message = messageSource.getMessage("escapedMessage", new Object[0], null);
		Assert.assertEquals("This is an 'escaped' message", message);

		message = messageSource.getMessage("escapedMessageWithParam", new Object[] { "John Doe" }, null);
		Assert.assertEquals("Hello, 'John Doe'!", message);

		Assert.assertEquals("bar", application.getMessage(environment.getLocale(), "foo"));

		Assert.assertNotNull(application.getBean("request", Request.class));
		DataSource datasource = application.getBean("datasource", DataSource.class);
		Assert.assertNotNull(datasource);
		Assert.assertNotNull(application.getBean("entityManager", EntityManager.class));

		TestService testService = application.getBean(TestService.class);
		TestEntity entity = new TestEntity("foo");
		testService.createEntity(entity);

		Assert.assertEquals("demo-application", application.getName());
		Assert.assertEquals("A demo Application", application.getDescription());
		Assert.assertEquals("Demo Application", application.getDisplayName());
		Assert.assertEquals("2012-11-27-1305", application.getTimestamp());
		Assert.assertEquals("1.5.2", application.getPackageVersion());
		Assert.assertEquals("This is an amazing demo application", application.getLongDescription());
		Assert.assertEquals("1.0.0-M1", application.getAppNGVersion());

		Resources applicationResources = application.getResources();
		Assert.assertNotNull(applicationResources.getResource(ResourceType.XML, "datasources.xml"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.XML, "events.xml"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.XML, "master.xml"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.XML, "page.xml"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.XML, "application.xml"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.DICTIONARY, "messages-demo.properties"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.SQL, "mssql/V1.0_script.sql"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.BEANS_XML, "beans.xml"));
		Assert.assertNotNull(applicationResources.getResource(ResourceType.APPLICATION, "application.xml"));

		assertRoleWithPermissions("Administrator", "output-format.html", "output-type.webgui");
		assertRoleWithPermissions("Debugger", "debug");
		Assert.assertEquals("bar", application.getProperties().getString("foo"));
	}

	private void assertRoleWithPermissions(String role, String... permissions) {
		List<String> permissionList = new ArrayList<>(Arrays.asList(permissions));
		for (Role applicationRole : application.getRoles()) {
			if (applicationRole.getName().equals(role)) {
				for (Permission permission : applicationRole.getPermissions()) {
					String name = permission.getName();
					permissionList.remove(name);
				}
			}
		}
		Assert.assertTrue(permissionList.isEmpty());
	}

}
