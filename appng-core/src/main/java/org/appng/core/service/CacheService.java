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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.spi.CachingProvider;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.appng.api.BusinessException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.core.controller.AppngCache;
import org.appng.core.domain.SiteImpl;

import com.hazelcast.cache.HazelcastCachingProvider;
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

	/**
	 * Returns the {@link CacheManager} instance.
	 * 
	 * @return The {@link CacheManager} instance.
	 */
	public static CacheManager createCacheManager(Properties cachingProps) {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		if (null == cachingProps) {
			cacheManager = cachingProvider.getCacheManager();
		} else {
			HazelcastInstance instance = HazelcastConfigurer.getInstance();
			if (instance == null) {
				instance = HazelcastConfigurer.configure(cachingProps);
			}
			Properties properties = new Properties();
			properties.put(HazelcastCachingProvider.HAZELCAST_INSTANCE_ITSELF, instance);
			cacheManager = cachingProvider.getCacheManager(null, null, properties);
		}
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
	 * @param ttl               the ttl for a cache element on seconds
	 * @param statisticsEnabled
	 * @return The {@link Cache} instance for the specified site.
	 */
	public synchronized static Cache<String, AppngCache> createCache(Site site, Integer ttl,
			Boolean statisticsEnabled) {
		String cacheKey = getCacheKey(site);
		Cache<String, AppngCache> cache = cacheManager.getCache(cacheKey);
		if (null == cache) {
			MutableConfiguration<String, AppngCache> configuration = new MutableConfiguration<>();
			Factory<ExpiryPolicy> expiryPolicy = AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, ttl));
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
		CacheManager cm = getCacheManager();
		for (String cacheName : cm.getCacheNames()) {
			cm.getCache(cacheName).close();
			LOGGER.info("Shutting down cache: {}", cacheName);
		}
	}

	public static Map<String, String> getCacheStatistics(SiteImpl site) {
		Map<String, String> stats = new HashMap<>();
		Boolean cacheEnabled = site.getProperties().getBoolean(SiteProperties.EHCACHE_ENABLED);
		if (cacheEnabled) {
			try {
				Cache<String, AppngCache> cache = CacheService.getCache(site);
				if (null != cache) {
					ObjectName objectName = new ObjectName(
							String.format("javax.cache:type=CacheStatistics,CacheManager=%s,Cache=%s",
									cache.getCacheManager().getURI(), cache.getName()));

					MBeanServer mbeans = ManagementFactory.getPlatformMBeanServer();
					stats.put("Average get time", getAttribute(mbeans, objectName, "AverageGetTime"));
					stats.put("Average put time", getAttribute(mbeans, objectName, "AveragePutTime"));
					stats.put("Average removal time", getAttribute(mbeans, objectName, "AverageRemoveTime"));
					stats.put("Hits", getAttribute(mbeans, objectName, "CacheHits"));
					stats.put("Misses", getAttribute(mbeans, objectName, "CacheMisses"));
					stats.put("Name", cache.getName());
					stats.put("Hits (%)", getAttribute(mbeans, objectName, "CacheHitPercentage"));
					stats.put("Misses (%)", getAttribute(mbeans, objectName, "CacheMissPercentage"));
					stats.put("Gets", getAttribute(mbeans, objectName, "CacheGets"));
					stats.put("Puts", getAttribute(mbeans, objectName, "CachePuts"));
					stats.put("Removals", getAttribute(mbeans, objectName, "CacheRemovals"));
				} else {
					stats.put("Status",
							"Statistics are disabled for this site. To enable statistics set the site property 'platform.site."
									+ site.getName() + ".ehcacheStatistics' to 'true'.");
				}
			} catch (Exception e) {
				LOGGER.error("Error while getting cache statistics.", e);
			}
		} else {
			stats.put("Status",
					"Ehcache is disabled for this site. To enable the cache set the site property 'platform.site."
							+ site.getName() + ".ehcacheEnabled' to 'true'.");
		}
		return stats;
	}

	private static String getAttribute(MBeanServer mBeanServer, ObjectName objectName, String attribute)
			throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException {
		return String.valueOf(mBeanServer.getAttribute(objectName, attribute));
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

	public static List<AppngCache> getCacheEntries(SiteImpl site) {
		List<AppngCache> appngCacheEntries = new ArrayList<>();
		try {
			Cache<String, AppngCache> cache = CacheService.getCache(site);
			if (null != cache) {
				for (javax.cache.Cache.Entry<String, AppngCache> entry : cache) {
					appngCacheEntries.add(entry.getValue());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while getting cache entries.", e);
		}
		return appngCacheEntries;
	}

	public static int expireCacheElementsStartingWith(Site site, String cacheElementPrefix) {
		int count = 0;
		try {
			Cache<String, AppngCache> cache = CacheService.getCache(site);
			for (javax.cache.Cache.Entry<String, AppngCache> entry : cache) {
				if (entry.getKey().startsWith(cacheElementPrefix)) {
					if (cache.remove(entry.getKey())) {
						count++;
					}
				}
			}
		} catch (IllegalStateException e) {
			LOGGER.error(String.format("Error while expiring cache entries starting with '%s'", cacheElementPrefix), e);
		}
		return count;
	}

}
