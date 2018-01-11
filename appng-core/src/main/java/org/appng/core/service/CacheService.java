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
package org.appng.core.service;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;

import org.appng.api.model.Site;

/**
 * Provides utility methods for the page cache.
 * 
 * @author Matthias Herlitzius
 *
 */
public class CacheService {

	public static final String PAGE_CACHE = "pageCache";
	public static final String DASH = "-";

	/**
	 * Returns the {@link CacheManager} instance.
	 * 
	 * @return The {@link CacheManager} instance.
	 */
	public static CacheManager getCacheManager() {
		return CacheManager.getInstance();
	}

	/**
	 * Returns the {@link BlockingCache} instance for the selected {@link Site}. Use this method to retrieve a cache
	 * instance which already must exists.
	 * 
	 * @param site
	 *            The {@link Site} to get the cache for
	 * @return The {@link BlockingCache} instance for the specified site.
	 */
	public static BlockingCache getBlockingCache(Site site) {
		return (BlockingCache) getCacheManager().getEhcache(getCacheKey(site));
	}

	public static void clearCache(Site site) {
		BlockingCache cache = getBlockingCache(site);
		if (null != cache) {
			cache.removeAll();
		}
	}

	public static void clearStatistics(Site site) {
		BlockingCache cache = getBlockingCache(site);
		if (null != cache) {
			cache.getStatistics().clearStatistics();
		}
	}

	/**
	 * Returns the {@link BlockingCache} instance for the selected {@link Site}. Use this method to retrieve a new cache
	 * instance. Should be only used in {@link InitializerService}
	 * 
	 * @param site
	 *            The site.
	 * @param blockingTimeoutMillis
	 *            The time, in milliseconds, to wait for the filter before a {@link LockTimeoutException} is thrown
	 * @return The {@link BlockingCache} instance for the specified site.
	 */
	synchronized static BlockingCache getBlockingCache(Site site, Integer blockingTimeoutMillis) {
		String cacheKey = getCacheKey(site);
		CacheManager cacheManager = getCacheManager();
		Ehcache cache = cacheManager.addCacheIfAbsent(cacheKey);
		if (!(cache instanceof BlockingCache)) {
			BlockingCache blockingCache = new BlockingCache(cache);
			blockingCache.setTimeoutMillis(blockingTimeoutMillis);
			cacheManager.replaceCacheWithDecoratedCache(cache, blockingCache);
		}
		return (BlockingCache) cacheManager.getEhcache(cacheKey);
	}

	private static String getCacheKey(Site site) {
		return new StringBuilder(PAGE_CACHE).append(DASH).append(site.getHost()).toString();
	}

}
