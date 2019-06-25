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
package org.appng.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

import org.appng.api.BusinessException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.core.controller.AppngCache;
import org.springframework.http.HttpMethod;

import com.hazelcast.cache.CacheStatistics;
import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.cache.ICache;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides utility methods for the page cache.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 *
 */
@Slf4j
public class CacheService {

	public static final String PAGE_CACHE = "pageCache";
	public static final String DASH = "-";
	private static CacheManager cacheManager;

	public static CacheManager createCacheManager(HazelcastInstance instance) {
		Properties properties = new Properties();
		properties.put(HazelcastCachingProvider.HAZELCAST_INSTANCE_ITSELF, instance);
		properties.put(HazelcastCachingProvider.HAZELCAST_CONFIG_LOCATION, "appNG configuration");
		cacheManager = Caching.getCachingProvider().getCacheManager(null, null, properties);
		return cacheManager;
	}

	/**
	 * Returns the {@link CacheManager} instance.
	 * 
	 * @return The {@link CacheManager} instance.
	 */
	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	/**
	 * Returns the {@link Cache} instance for the selected {@link Site}. Use this
	 * method to retrieve a cache instance which already must exists.
	 * 
	 * @param site The {@link Site} to get the cache for
	 * @return The {@link Cache} instance for the specified site.
	 */
	public static Cache<String, AppngCache> getCache(Site site) {
		return cacheManager.getCache(getCacheKey(site));
	}

	public static void clearCache(Site site) {
		Cache<String, AppngCache> cache = getCache(site);
		if (null != cache) {
			cache.removeAll();
		}
	}

	public static void clearStatistics(Site site) {
	}

	/**
	 * Returns the {@link Cache} instance for the selected {@link Site}. Use this
	 * method to retrieve a new cache instance. Should be only used in
	 * {@link InitializerService}
	 * 
	 * @param site              The site.
	 * @return The {@link Cache} instance for the specified site.
	 */
	public synchronized static Cache<String, AppngCache> createCache(Site site) {
		String cacheKey = getCacheKey(site);
		Cache<String, AppngCache> cache = cacheManager.getCache(cacheKey);
		if (null == cache) {
			Integer ttl = site.getProperties().getInteger(SiteProperties.CACHE_TIME_TO_LIVE);
			Boolean statisticsEnabled = site.getProperties().getBoolean(SiteProperties.CACHE_STATISTICS);
			MutableConfiguration<String, AppngCache> configuration = new MutableConfiguration<>();
			Duration duration = new Duration(TimeUnit.SECONDS, ttl);
			Factory<ExpiryPolicy> expiryPolicy;
			if (site.getProperties().getBoolean(SiteProperties.CACHE_EXPIRE_BY_CREATION, true)) {
				expiryPolicy = CreatedExpiryPolicy.factoryOf(duration);
			} else {
				expiryPolicy = AccessedExpiryPolicy.factoryOf(duration);
			}
			configuration.setExpiryPolicyFactory(expiryPolicy);
			configuration.setStatisticsEnabled(statisticsEnabled);
			cache = cacheManager.createCache(cacheKey, configuration);
		}
		return cache;
	}

	private static String getCacheKey(Site site) {
		return new StringBuilder(PAGE_CACHE).append(DASH).append(site.getHost()).toString();
	}

	public static void shutdown() {
		for (String cacheName : cacheManager.getCacheNames()) {
			cacheManager.getCache(cacheName).close();
			LOGGER.info("Shutting down cache: {}", cacheName);
		}
	}

	public static Map<String, String> getCacheStatistics(Site site) {
		Map<String, String> stats = new HashMap<>();
		Boolean cacheEnabled = site.getProperties().getBoolean(SiteProperties.CACHE_ENABLED);
		if (cacheEnabled) {
			try {
				Cache<String, AppngCache> cache = CacheService.getCache(site);
				if (null != cache) {
					@SuppressWarnings("unchecked")
					ICache<String, AppngCache> cacheInternal = cache.unwrap(ICache.class);
					CacheStatistics cacheStatistics = cacheInternal.getLocalCacheStatistics();

					stats.put("Average get time", String.valueOf(cacheStatistics.getAverageGetTime()));
					stats.put("Average put time", String.valueOf(cacheStatistics.getAveragePutTime()));
					stats.put("Average removal time", String.valueOf(cacheStatistics.getAverageRemoveTime()));
					stats.put("Hits", String.valueOf(cacheStatistics.getCacheHits()));
					stats.put("Misses", String.valueOf(cacheStatistics.getCacheMisses()));
					stats.put("Name", cache.getName());
					stats.put("Hits (%)", String.valueOf(cacheStatistics.getCacheHitPercentage()));
					stats.put("Misses (%)", String.valueOf(cacheStatistics.getCacheMissPercentage()));
					stats.put("Gets", String.valueOf(cacheStatistics.getCacheGets()));
					stats.put("Puts", String.valueOf(cacheStatistics.getCachePuts()));
					stats.put("Removals", String.valueOf(cacheStatistics.getCacheRemovals()));
				} else {
					stats.put("Status",
							String.format("Failed to retrieve caching statistics for site %s", site.getName()));
				}
			} catch (Exception e) {
				LOGGER.error("Error while getting cache statistics.", e);
			}
		} else {
			stats.put("Status",
					"Ehcache is disabled for this site. To enable the cache set the site property 'platform.site."
							+ site.getName() + ".cacheEnabled' to 'true'.");
		}
		return stats;
	}

	public static void expireCacheElement(Site site, String cacheElement) {
		boolean hasRemoved;
		try {
			hasRemoved = getCache(site).remove(cacheElement);
			if (!hasRemoved) {
				throw new BusinessException("No such element: " + cacheElement);
			}
		} catch (Exception e) {
			LOGGER.error(String.format("Error while expiring cache entry: %s", cacheElement), e);
		}
	}

	public static List<AppngCache> getCacheEntries(Site site) {
		List<AppngCache> appngCacheEntries = new ArrayList<>();
		try {
			Cache<String, AppngCache> cache = CacheService.getCache(site);
			if (null != cache) {
				for (Entry<String, AppngCache> entry : cache) {
					appngCacheEntries.add(entry.getValue());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while getting cache entries.", e);
		}
		return appngCacheEntries;
	}

	public static int expireCacheElementsStartingWith(Site site, String cacheElementPrefix) {
		return expireCacheElementsStartingWith(CacheService.getCache(site), cacheElementPrefix);
	}

	public static int expireCacheElementsStartingWith(Cache<String, AppngCache> cache, String cacheElementPrefix) {
		int count = 0;
		int removed = 0;
		for (Entry<String, AppngCache> entry : cache) {
			count++;
			if (entry.getKey().startsWith(HttpMethod.GET.name() + cacheElementPrefix)) {
				if (cache.remove(entry.getKey())) {
					LOGGER.debug("removed from cache: {}", entry.getKey());
					removed++;
				}
			}
		}
		LOGGER.info("removed {} cache elements for {} (cache size: {})", removed, cacheElementPrefix, count);
		return count;
	}

}
