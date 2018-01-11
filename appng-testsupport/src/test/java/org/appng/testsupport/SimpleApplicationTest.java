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

import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.Permissions;
import org.appng.xml.application.Property;
import org.appng.xml.application.Roles;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;

public class SimpleApplicationTest {

	@Test
	public void testProperties() {
		TestBase.SimpleApplication simpleApplication = new TestBase().new SimpleApplication("demo-app",
				Mockito.mock(ConfigurableApplicationContext.class));
		ApplicationInfo applicationInfo = new ApplicationInfo();
		applicationInfo.setPermissions(new Permissions());
		applicationInfo.setRoles(new Roles());
		org.appng.xml.application.Properties appProps = new org.appng.xml.application.Properties();
		applicationInfo.setProperties(appProps);

		Property regular = new Property();
		regular.setId("regular");
		regular.setValue("foo");
		appProps.getProperty().add(regular);

		Property multilined = new Property();
		multilined.setId("multilined");
		multilined.setValue("foo\nbar");
		multilined.setClob(true);
		appProps.getProperty().add(multilined);

		Property override = new Property();
		override.setId("override");
		override.setValue("willBeOverridden");
		appProps.getProperty().add(override);

		java.util.Properties overrides = new java.util.Properties();
		overrides.put(override.getId(), "theNewValue");

		simpleApplication.init(overrides, applicationInfo);

		org.appng.api.model.Properties properties = simpleApplication.getProperties();
		Assert.assertEquals("foo", properties.getString(regular.getId()));
		Assert.assertNull(properties.getClob(regular.getId()));
		Assert.assertEquals("foo\nbar", properties.getClob(multilined.getId()));
		Assert.assertNull(properties.getString(multilined.getId()));
		Assert.assertEquals("theNewValue", properties.getString(override.getId()));
		Assert.assertNull(properties.getClob(override.getId()));
	}
}
