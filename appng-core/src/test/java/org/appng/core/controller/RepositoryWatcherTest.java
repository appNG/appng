/*
 * Copyright 2011-2019 the original author or authors.
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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.Cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.CacheService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

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
		CacheService.createCacheManager(new Properties());
		Cache<String, AppngCacheElement> cache = CacheService.createCache(site, 1800, true);

		String fehlerJsp = "/de/fehler.jsp";
		String testJsp = "/de/test.jsp";
		String keyFehlerJsp = "GET" + fehlerJsp;
		String keyTestJsp = "GET" + testJsp;
		cache.put(keyFehlerJsp, new AppngCacheElement(200, "text/plain", "a value".getBytes(), new HttpHeaders()));
		cache.put(keyTestJsp, new AppngCacheElement(200, "text/plain", "a value".getBytes(), new HttpHeaders()));
		cache.put("GET/de/error", new AppngCacheElement(200, "text/plain", "a value".getBytes(), new HttpHeaders()));
		cache.put("GET/de/fault", new AppngCacheElement(200, "text/plain", "a value".getBytes(), new HttpHeaders()));

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
			Thread.sleep(100);
		}
		Assert.assertNull(cache.get(keyFehlerJsp));
		Assert.assertNull(cache.get(keyTestJsp));
		Assert.assertNull(cache.get("GET/de/error"));
		Assert.assertNull(cache.get("GET/de/fault"));
		Assert.assertEquals(0, getCacheSize(cache));
		Assert.assertTrue(repositoryWatcher.forwardsUpdatedAt > forwardsUpdatedAt);
	}

	private int getCacheSize(Cache<String, AppngCacheElement> cache) {
		AtomicInteger size=new AtomicInteger(0);
		cache.iterator().forEachRemaining(e->size.getAndIncrement());
		return size.get();
	}
}
