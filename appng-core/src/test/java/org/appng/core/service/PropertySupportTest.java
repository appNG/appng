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
package org.appng.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.appng.api.Platform;
import org.appng.api.SiteProperties;
import org.appng.api.model.Property;
import org.appng.api.support.PropertyHolder;
import org.appng.core.domain.SiteImpl;
import org.junit.Assert;
import org.junit.Test;

public class PropertySupportTest {

	@Test
	public void testInitPlatformConfig() {
		Properties defaultOverrides = new Properties();
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + "a", "1");
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + "b", "foo");
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + "c", "foo\nbar");
		defaultOverrides.put("dummy", "foo");
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + Platform.Property.FILEBASED_DEPLOYMENT, "false");
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + Platform.Property.MAIL_DISABLED, "false");
		PropertyHolder platformProps = getPlatformConfig(defaultOverrides);

		Assert.assertFalse(platformProps.getBoolean(Platform.Property.FILEBASED_DEPLOYMENT));
		Assert.assertFalse(platformProps.getBoolean(Platform.Property.MAIL_DISABLED));
		Assert.assertTrue(platformProps.getBoolean(Platform.Property.DEV_MODE));
		Assert.assertEquals(Integer.valueOf(1), platformProps.getInteger("a"));
		Assert.assertEquals("foo", platformProps.getString("b"));
		Assert.assertEquals("foo\nbar", platformProps.getClob("c"));
		Assert.assertNull(platformProps.getString("dummy"));
	}

	private PropertyHolder getPlatformConfig(Properties defaultOverrides) {
		List<Property> properties = new ArrayList<>();
		PropertyHolder propertyHolder = new PropertyHolder(PropertySupport.PREFIX_PLATFORM, properties);
		PropertySupport propertySupport = new PropertySupport(propertyHolder);
		propertySupport.initPlatformConfig("rootPath", true, defaultOverrides, true);
		return propertyHolder;
	}

	@Test
	public void testInitSiteProperties() {
		List<Property> properties = new ArrayList<>();
		PropertyHolder siteProps = new PropertyHolder(PropertySupport.PREFIX_SITE, properties);
		PropertySupport propertySupport = new PropertySupport(siteProps);
		SiteImpl site = new SiteImpl();
		site.setName("localhost");
		site.setHost("localhost");
		propertySupport.initSiteProperties(site, getPlatformConfig(new Properties()));
		Assert.assertEquals(Boolean.FALSE, siteProps.getObject(SiteProperties.CACHE_ENABLED));
		Assert.assertFalse(siteProps.getBoolean(SiteProperties.CACHE_ENABLED));
		Assert.assertEquals("localhost", siteProps.getObject(SiteProperties.HOST));
		Assert.assertEquals("localhost", siteProps.getString(SiteProperties.HOST));
	}

}
