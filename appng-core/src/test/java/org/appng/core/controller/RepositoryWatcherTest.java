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
package org.appng.core.controller;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class RepositoryWatcherTest {

	@Test
	public void test() throws Exception {
		ClassLoader classLoader = RepositoryWatcherTest.class.getClassLoader();
		String rootDir = classLoader.getResource("repository/manager/www").getFile();
		String urlrewrite = classLoader.getResource("conf/urlrewrite.xml").getFile();

		RepositoryWatcher repositoryWatcher = new RepositoryWatcher();
		Cache ehcache = new Cache("testcache", 1000, MemoryStoreEvictionPolicy.LRU, false, null, false, 1000, 1000,
				false, 60, new RegisteredEventListeners(null));
		Configuration configuration = new Configuration();
		configuration.setName(getClass().getSimpleName() + "cache");
		ehcache.setCacheManager(new CacheManager(configuration));
		ehcache.initialise();
		String fehlerJsp = "/de/fehler.jsp";
		String testJsp = "/de/test.jsp";
		String keyFehlerJsp = "GET" + fehlerJsp;
		String keyTestJsp = "GET" + testJsp;
		ehcache.put(new Element(keyFehlerJsp, "a value"));
		ehcache.put(new Element(keyTestJsp, "a value"));
		ehcache.put(new Element("GET/de/error", "a value"));
		ehcache.put(new Element("GET/de/fault", "a value"));
		Assert.assertEquals(4, ehcache.getSize());
		repositoryWatcher.init(ehcache, rootDir, new File(urlrewrite), RepositoryWatcher.DEFAULT_RULE_SUFFIX,
				Arrays.asList("de"));
		ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat("repositoryWatcher").setDaemon(true).build();
		ExecutorService executor = Executors.newSingleThreadExecutor(tf);
		executor.execute(repositoryWatcher);

		FileUtils.touch(new File(rootDir, fehlerJsp));
		FileUtils.touch(new File(rootDir, testJsp));
		Thread.sleep(200);
		Assert.assertNull(ehcache.get(keyFehlerJsp));
		Assert.assertNull(ehcache.get(keyTestJsp));
		Assert.assertNull(ehcache.get("GET/de/error"));
		Assert.assertNull(ehcache.get("GET/de/fault"));
		Assert.assertEquals(0, ehcache.getSize());
	}
}
