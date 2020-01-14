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
package org.appng.core.model;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.appng.api.Platform;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class CacheProviderTest {

	private CacheProvider cacheProvider;

	private String applicationName = "foobar";

	private String sitename = "appNG";

	@Mock
	private Properties properties;

	@Mock
	private Site site;

	@Mock
	private Application application;

	private File cacheRoot = new File("target/WEB-INF/cache");

	private File applicationRoot = new File(cacheRoot, "application").getAbsoluteFile();
	private File platformRoot = new File(cacheRoot, "platform").getAbsoluteFile();

	private File applicationCache = new File(applicationRoot, sitename + "/" + applicationName).getAbsoluteFile();
	private File platformCache = new File(platformRoot, sitename + "/" + applicationName).getAbsoluteFile();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(site.getName()).thenReturn(sitename);
		Mockito.when(application.getName()).thenReturn(applicationName);
		Mockito.when(properties.getString(Platform.Property.PLATFORM_ROOT_PATH)).thenReturn("target");
		Mockito.when(properties.getString(Platform.Property.CACHE_FOLDER)).thenReturn("cache");
		Mockito.when(properties.getString(Platform.Property.PLATFORM_CACHE_FOLDER)).thenReturn("platform");
		Mockito.when(properties.getString(Platform.Property.APPLICATION_CACHE_FOLDER)).thenReturn("application");
		Mockito.when(properties.getString(Platform.Property.IMAGE_CACHE_FOLDER)).thenReturn("images");

		FileUtils.deleteQuietly(cacheRoot);
		Assert.assertFalse(platformCache.exists());
		Assert.assertFalse(applicationCache.exists());
		this.cacheProvider = new CacheProvider(properties);
		cacheProvider.getApplicationCache(site, application);
		cacheProvider.getPlatformCache(site, application);
		Assert.assertTrue(platformCache.exists());
		Assert.assertTrue(applicationCache.exists());
	}

	@Test
	public void testgetCache() {
		File cache = cacheProvider.getCache();
		Assert.assertEquals(cacheRoot, cache);
	}

	@Test
	public void testClearCache() {
		File platformSite = new File("target/WEB-INF/cache/platform/" + sitename);
		File appSite = new File("target/WEB-INF/cache/application/" + sitename);
		Assert.assertTrue(platformSite.exists());
		Assert.assertTrue(appSite.exists());
		cacheProvider.clearCache(site);
		Assert.assertFalse(platformSite.exists());
		Assert.assertFalse(appSite.exists());
	}

	@Test
	public void testClearApplicationCache() {
		Assert.assertTrue(platformCache.exists());
		Assert.assertTrue(applicationCache.exists());
		cacheProvider.clearCache(site, applicationName);
		Assert.assertTrue(applicationRoot.exists());
		Assert.assertTrue(platformRoot.exists());
		Assert.assertFalse(platformCache.exists());
		Assert.assertFalse(applicationCache.exists());
	}

	@Test
	public void testgetPlatformCache() {
		FileUtils.deleteQuietly(platformCache);
		File cache = cacheProvider.getPlatformCache(sitename, applicationName).getAbsoluteFile();
		Assert.assertEquals(platformCache, cache);
	}

	@Test
	public void testGetRelativePlatformCache() {
		String cache = cacheProvider.getRelativePlatformCache(site, application);
		Assert.assertEquals(new File("/WEB-INF/cache/platform/appNG/foobar").getPath(), cache);
	}

	@Test
	public void testGetImageCache() {
		File cache = cacheProvider.getImageCache(site, application).getAbsoluteFile();
		Assert.assertEquals(new File(applicationCache, "images"), cache);
	}

}
