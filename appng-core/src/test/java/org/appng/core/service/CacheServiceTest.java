/*
 * Copyright 2011-2022 the original author or authors.
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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Future;

import javax.cache.Cache;

import org.apache.commons.lang3.ArrayUtils;
import org.appng.api.SiteProperties;
import org.appng.api.model.SimpleProperty;
import org.appng.api.support.PropertyHolder;
import org.appng.core.controller.CachedResponse;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.cache.CacheEntryListener;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StopWatch;

import com.hazelcast.config.CacheConfig;

public class CacheServiceTest {

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void test() throws Exception {
		CacheService.createCacheManager(HazelcastConfigurer.getInstance(null), false);
		SiteImpl site = new SiteImpl();
		site.setHost("appng");
		PropertyHolder properties = new PropertyHolder("",
				Arrays.asList(new SimpleProperty(SiteProperties.CACHE_TIME_TO_LIVE, "1800"),
						new SimpleProperty(SiteProperties.CACHE_STATISTICS, "true"),
						new SimpleProperty(CacheService.CACHE_USE_ENTRY_LISTENER, "true")));
		site.setProperties(properties);
		Cache<String, CachedResponse> cache = CacheService.createCache(site);
		CacheConfig config = cache.getConfiguration(CacheConfig.class);
		StopWatch sw = new StopWatch();
		sw.start("Add entries");
		byte[] data = Files.readAllBytes(new File("src/test/resources/hazelcast.xml").toPath());

		CacheEntryListener listener = CacheService.getCacheEntryListener(cache);
		Assert.assertEquals(0, listener.getKeys().size());

		Integer items = 200;
		String prefix = "/item";
		for (int i = 0; i < items; i++) {
			String key = "GET" + prefix + i;
			cache.put(key, new CachedResponse(key, site, new MockHttpServletRequest(), 200, "application/xml",
					ArrayUtils.clone(data), new HttpHeaders(), 1800));
		}
		Assert.assertEquals(items, Integer.valueOf(listener.getKeys().size()));
		sw.stop();
		sw.start("Expire entries");
		Future<Integer> expireCacheElementsByPrefix = CacheService.expireCacheElementsByPrefix(site, prefix);
		Integer expired = expireCacheElementsByPrefix.get();
		Assert.assertEquals(items, expired);
		sw.stop();
		Assert.assertEquals(0, listener.getKeys().size());

		Assert.assertEquals(config, CacheService.createCache(site).getConfiguration(CacheConfig.class));

		properties = new PropertyHolder("",
				Arrays.asList(new SimpleProperty(SiteProperties.CACHE_TIME_TO_LIVE, "1800"),
						new SimpleProperty(SiteProperties.CACHE_EXPIRE_ELEMENTS_BY_CREATION, "true"),
						new SimpleProperty(SiteProperties.CACHE_STATISTICS, "true"),
						new SimpleProperty(CacheService.CACHE_USE_ENTRY_LISTENER, "true")));
		site.setProperties(properties);
		Assert.assertNotEquals(cache, CacheService.createCache(site).getConfiguration(CacheConfig.class));

	}

}
