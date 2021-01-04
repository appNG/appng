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

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.Cache;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.appng.api.SiteProperties;
import org.appng.api.model.Property;
import org.appng.api.support.PropertyHolder;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.CacheService;
import org.appng.core.service.HazelcastConfigurer;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RepositoryWatcherTest {

	@Test(timeout = 100000)
	public void test() throws Exception {
		ClassLoader classLoader = RepositoryWatcherTest.class.getClassLoader();
		URL url = classLoader.getResource("repository/manager/www");
		String rootDir = FilenameUtils.normalize(new File(url.toURI()).getPath(), true);
		String urlrewrite = classLoader.getResource("conf/urlrewrite.xml").getFile();

		RepositoryWatcher repositoryWatcher = new RepositoryWatcher();
		SiteImpl site = new SiteImpl();
		site.setHost("localhost");
		PropertyHolder siteProps = new PropertyHolder();
		siteProps.addProperty(SiteProperties.CACHE_TIME_TO_LIVE, 1800, null, Property.Type.INT);
		siteProps.addProperty(SiteProperties.CACHE_STATISTICS, true, null, Property.Type.BOOLEAN);
		site.setProperties(siteProps);

		CacheService.createCacheManager(HazelcastConfigurer.getInstance(null), false);
		Cache<String, CachedResponse> cache = CacheService.createCache(site);

		String fehlerJsp = "/de/fehler.jsp";
		String testJsp = "/de/test.jsp";
		String keyFehlerJsp = "GET" + fehlerJsp;
		String keyTestJsp = "GET" + testJsp;
		int timeToLive = 1800;
		HttpServletRequest req = new MockHttpServletRequest();
		String contentType = "text/plain";
		byte[] bytes = "a value".getBytes();
		cache.put(keyFehlerJsp,
				new CachedResponse(keyFehlerJsp, site, req, 200, contentType, bytes, new HttpHeaders(), timeToLive));
		cache.put(keyTestJsp,
				new CachedResponse(keyTestJsp, site, req, 200, contentType, bytes, new HttpHeaders(), timeToLive));
		cache.put("GET/de/error",
				new CachedResponse("GET/de/error", site, req, 200, contentType, bytes, new HttpHeaders(), timeToLive));
		cache.put("GET/de/fault",
				new CachedResponse("GET/de/fault", site, req, 200, contentType, bytes, new HttpHeaders(), timeToLive));

		int size = getCacheSize(cache);
		Assert.assertEquals(4, size);
		repositoryWatcher.init(cache, rootDir, new File(urlrewrite), RepositoryWatcher.DEFAULT_RULE_SUFFIX,
				Arrays.asList("de"));
		Long forwardsUpdatedAt = repositoryWatcher.forwardsUpdatedAt;
		ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat("repositoryWatcher").setDaemon(true).build();
		ExecutorService executor = Executors.newSingleThreadExecutor(tf);
		executor.execute(repositoryWatcher);

		FileUtils.touch(new File(rootDir, fehlerJsp));
		FileUtils.touch(new File(rootDir, testJsp));
		FileUtils.touch(new File(urlrewrite));
		while (getCacheSize(cache) != 0 || forwardsUpdatedAt == repositoryWatcher.forwardsUpdatedAt) {
			Thread.sleep(50);
		}
		Assert.assertNull(cache.get(keyFehlerJsp));
		Assert.assertNull(cache.get(keyTestJsp));
		Assert.assertNull(cache.get("GET/de/error"));
		Assert.assertNull(cache.get("GET/de/fault"));
		Assert.assertEquals(0, getCacheSize(cache));
		Assert.assertTrue(repositoryWatcher.forwardsUpdatedAt > forwardsUpdatedAt);
	}

	private int getCacheSize(Cache<String, CachedResponse> cache) {
		AtomicInteger size = new AtomicInteger(0);
		cache.iterator().forEachRemaining(e -> size.getAndIncrement());
		return size.get();
	}
}
