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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.appng.api.BusinessException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.controller.CachedResponse;
import org.appng.core.service.cache.CacheEntryListener;
import org.springframework.http.HttpMethod;

import com.google.common.collect.Streams;
import com.hazelcast.cache.CacheStatistics;
import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.cache.ICache;
import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.client.cache.impl.HazelcastClientCachingProvider;
import com.hazelcast.config.CacheConfig;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides utility methods for the page cache.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
public class CacheService {

	static final String CACHE_USE_ENTRY_LISTENER = "cacheUseEntryListener";
	public static final String PAGE_CACHE = "pageCache";
	public static final String DASH = "-";
	// hidden site property
	private static final String CACHE_MAX_SIZE = "cacheMaxSize";
	private static final int DEFAULT_MAX_SIZE = 20000;

	public static final String STATS_NAME = "name";
	public static final String STATS_SIZE = "size";
	public static final String STATS_PUTS = "puts";
	public static final String STATS_GETS = "gets";
	public static final String STATS_REMOVALS = "removals";
	public static final String STATS_HITS = "hits";
	public static final String STATS_HITS_PERCENT = "hitsPercent";
	public static final String STATS_MISSES = "misses";
	public static final String STATS_MISSES_PERCENT = "missesPercent";
	public static final String STATS_AVG_PUT_TIME = "avgPutTime";
	public static final String STATS_AVG_GET_TIME = "avgGetTime";
	public static final String STATS_AVG_REMOVAL_TIME = "avgRemovalTime";

	private static CacheManager cacheManager;
	private static final int MICROS_PER_MILLI = 1000;

	public static CacheManager createCacheManager(HazelcastInstance instance, boolean isClient) {
		java.util.Properties properties = new java.util.Properties();
		properties.put(HazelcastCachingProvider.HAZELCAST_INSTANCE_ITSELF, instance);
		properties.put(HazelcastCachingProvider.HAZELCAST_CONFIG_LOCATION, "appNG configuration");
		Class<?> cacheProviderClass = isClient ? HazelcastClientCachingProvider.class
				: HazelcastServerCachingProvider.class;
		cacheManager = Caching.getCachingProvider(cacheProviderClass.getName()).getCacheManager(null, null, properties);
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
	 * Returns the {@link Cache} instance for the selected {@link Site}. Use this method to retrieve a cache instance
	 * which already must exists.
	 * 
	 * @param  site
	 *              The {@link Site} to get the cache for
	 * 
	 * @return      The {@link Cache} instance for the specified site.
	 */
	public static Cache<String, CachedResponse> getCache(Site site) {
		return cacheManager.getCache(getCacheKey(site));
	}

	public static void clearCache(Site site) {
		Cache<String, CachedResponse> cache = getCache(site);
		if (null != cache) {
			cache.removeAll();
			getCacheEntryListener(cache).clear();
		}
	}

	public static void clearStatistics(Site site) {
	}

	/**
	 * Returns the {@link Cache} instance for the selected {@link Site}. Use this method to retrieve a new cache
	 * instance. Should be only used in {@link InitializerService}
	 * 
	 * @param  site
	 *              The site.
	 * 
	 * @return      The {@link Cache} instance for the specified site.
	 */
	@SuppressWarnings("unchecked")
	public synchronized static Cache<String, CachedResponse> createCache(Site site) {
		Properties siteProps = site.getProperties();
		Boolean statisticsEnabled = siteProps.getBoolean(SiteProperties.CACHE_STATISTICS);
		Integer ttl = siteProps.getInteger(SiteProperties.CACHE_TIME_TO_LIVE);
		Integer maxSize = siteProps.getInteger(CACHE_MAX_SIZE, DEFAULT_MAX_SIZE);
		Duration duration = new Duration(TimeUnit.SECONDS, ttl);
		Boolean expireByCreation = siteProps.getBoolean(SiteProperties.CACHE_EXPIRE_ELEMENTS_BY_CREATION, false);
		Factory<ExpiryPolicy> factory = expireByCreation ? CreatedExpiryPolicy.factoryOf(duration)
				: AccessedExpiryPolicy.factoryOf(duration);

		String cacheKey = getCacheKey(site);
		Cache<String, CachedResponse> cache = cacheManager.getCache(cacheKey);
		if (null != cache) {
			CacheConfig<String, CachedResponse> configuration = (CacheConfig<String, CachedResponse>) cache
					.getConfiguration(CacheConfig.class);
			ExpiryPolicy currentPolicy = configuration.getExpiryPolicyFactory().create();
			if ((configuration.isStatisticsEnabled() ^ statisticsEnabled)
					|| (configuration.getEvictionConfig().getSize() != maxSize)
					|| (currentPolicy.getExpiryForCreation().getDurationAmount() != ttl)
					|| !(currentPolicy.getClass().equals(factory.create().getClass()))) {
				cacheManager.destroyCache(cacheKey);
				cache = null;
				LOGGER.info("ttl, statistics, expiry policy or cache size has changed, destroyed cache '{}'.",
						cacheKey);
			}
		}

		if (null == cache) {
			CacheConfig<String, CachedResponse> configuration = new CacheConfig<>(cacheKey);
			configuration.setExpiryPolicyFactory(factory);
			configuration.setStatisticsEnabled(statisticsEnabled);
			configuration.setEvictionConfig(new EvictionConfig().setEvictionPolicy(EvictionPolicy.LRU).setSize(maxSize)
					.setMaxSizePolicy(MaxSizePolicy.ENTRY_COUNT));
			if (useCacheEntryListener(siteProps)) {
				CacheEntryListener cacheEntryListener = new CacheEntryListener();
				CacheEntryListenerConfiguration<String, CachedResponse> listenerConfiguration = new MutableCacheEntryListenerConfiguration<>(
						FactoryBuilder.factoryOf(cacheEntryListener), null, false, true);
				configuration.addCacheEntryListenerConfiguration(listenerConfiguration);
			}
			cache = cacheManager.createCache(cacheKey, configuration);
			LOGGER.info("Created cache '{}' (ttl: {}s (with {}), maximum entries: {}, statistics: {})", cacheKey, ttl,
					factory.create().getClass().getSimpleName(), maxSize, statisticsEnabled);
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
				Cache<String, CachedResponse> cache = CacheService.getCache(site);
				if (null != cache) {
					@SuppressWarnings("unchecked")
					ICache<String, CachedResponse> cacheInternal = cache.unwrap(ICache.class);
					CacheStatistics cacheStats = cacheInternal.getLocalCacheStatistics();

					stats.put(STATS_NAME, cache.getName());
					stats.put(STATS_SIZE, String.valueOf(cacheInternal.size()));
					stats.put(STATS_HITS, String.valueOf(cacheStats.getCacheHits()));
					stats.put(STATS_HITS_PERCENT, String.valueOf(cacheStats.getCacheHitPercentage()));
					stats.put(STATS_MISSES, String.valueOf(cacheStats.getCacheMisses()));
					stats.put(STATS_MISSES_PERCENT, String.valueOf(cacheStats.getCacheMissPercentage()));
					stats.put(STATS_PUTS, String.valueOf(cacheStats.getCachePuts()));
					stats.put(STATS_AVG_PUT_TIME, String.valueOf(cacheStats.getAveragePutTime() / MICROS_PER_MILLI));
					stats.put(STATS_GETS, String.valueOf(cacheStats.getCacheGets()));
					stats.put(STATS_AVG_GET_TIME, String.valueOf(cacheStats.getAverageGetTime() / MICROS_PER_MILLI));
					stats.put(STATS_REMOVALS, String.valueOf(cacheStats.getCacheRemovals()));
					stats.put(STATS_AVG_REMOVAL_TIME,
							String.valueOf(cacheStats.getAverageRemoveTime() / MICROS_PER_MILLI));
				} else {
					stats.put("Status",
							String.format("Failed to retrieve caching statistics for site %s", site.getName()));
				}
			} catch (Exception e) {
				LOGGER.error("Error while getting cache statistics.", e);
			}
		} else {
			stats.put("Status", "Caching is disabled for this site. To enable the cache set the site property '"
					+ SiteProperties.CACHE_ENABLED + "' to 'true'.");
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

	public static List<CachedResponse> getCacheEntries(Site site) {
		List<CachedResponse> appngCacheEntries = new ArrayList<>();
		try {
			Cache<String, CachedResponse> cache = CacheService.getCache(site);
			if (null != cache) {
				for (Entry<String, CachedResponse> entry : cache) {
					appngCacheEntries.add(entry.getValue());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while getting cache entries.", e);
		}
		return appngCacheEntries;
	}

	/**
	 * Expires cache elements by path prefix
	 * 
	 * @param      site
	 *                                the {@link Site} to retrieve the cache for
	 * @param      cacheElementPrefix
	 *                                the prefix to use
	 * 
	 * @return                        always {@code 0}, as the execution is asynchronous
	 * 
	 * @deprecated                    use {@link #expireCacheElementsByPrefix(Cache, String)} instead.
	 */
	@Deprecated
	public static int expireCacheElementsStartingWith(Site site, String cacheElementPrefix) {
		expireCacheElementsByPrefix(site, cacheElementPrefix);
		return 0;
	}

	/**
	 * Expires cache elements by path prefix
	 * 
	 * @param      cache
	 *                                the cache to use
	 * @param      cacheElementPrefix
	 *                                the prefix to use
	 * 
	 * @return                        always {@code 0}, as the execution is asynchronous
	 * 
	 * @deprecated                    Use {@link #expireCacheElementsByPrefix(Cache, String)} instead.
	 */
	@Deprecated
	public static int expireCacheElementsStartingWith(Cache<String, CachedResponse> cache, String cacheElementPrefix) {
		expireCacheElementsByPrefix(cache, cacheElementPrefix);
		return 0;
	}

	/**
	 * Expires cache elements by path prefix
	 * 
	 * @param  cache
	 *                            the cache to use
	 * @param  cacheElementPrefix
	 *                            the prefix to use
	 * 
	 * @return                    a {@link Future} holding the number of removed elements
	 */
	public static Future<Integer> expireCacheElementsByPrefix(Cache<String, CachedResponse> cache,
			String cacheElementPrefix) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Expires cache elements by path prefix
	 * 
	 * @param  site
	 *                            the {@link Site} to retrieve the cache for
	 * @param  cacheElementPrefix
	 *                            the prefix to use
	 * 
	 * @return                    a {@link Future} holding the number of removed elements
	 */
	public static Future<Integer> expireCacheElementsByPrefix(Site site, String cacheElementPrefix) {
		Cache<String, CachedResponse> cache = getCache(site);
		if (null == cache) {
			LOGGER.info("No cache found, can not remove elements starting with {}", cacheElementPrefix);
		} else {
			ExecutorService clearCache = Executors
					.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern(cache.getName()).build());
			Future<Integer> removedFuture = clearCache.submit(() -> {
				final long start = System.currentTimeMillis();
				int count = 0;
				final String completePrefix = HttpMethod.GET.name() + cacheElementPrefix;

				Set<String> keys = null;
				CacheEntryListener listener = getCacheEntryListener(cache);

				if (!useCacheEntryListener(site.getProperties()) || null == listener) {
					count = cache.unwrap(ICache.class).size();
					keys = Streams.stream(cache.iterator()).map(Entry::getKey).filter(k -> k.startsWith(completePrefix))
							.collect(Collectors.toSet());

				} else {
					count = listener.getKeys().size();
					keys = listener.getKeys(completePrefix);
				}

				AtomicInteger removed = new AtomicInteger();
				keys.parallelStream().forEach(key -> {
					if (cache.remove(key)) {
						LOGGER.debug("removed from cache: {}", key);
						removed.getAndIncrement();
					}
				});

				LOGGER.info("removed {} cache elements for {} in {}ms (previous cache size: {})", removed,
						cacheElementPrefix, System.currentTimeMillis() - start, count);
				return removed.get();
			});
			clearCache.shutdown();
			return removedFuture;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static CacheEntryListener getCacheEntryListener(Cache<String, CachedResponse> cache) {
		CacheConfig<String, CachedResponse> configuration = (CacheConfig<String, CachedResponse>) cache
				.getConfiguration(CacheConfig.class);
		Iterator<CacheEntryListenerConfiguration<String, CachedResponse>> listenerConfigs = configuration
				.getListenerConfigurations().iterator();
		if (listenerConfigs.hasNext()) {
			return (CacheEntryListener) listenerConfigs.next().getCacheEntryListenerFactory().create();
		}
		return null;
	}

	private static Boolean useCacheEntryListener(Properties siteProps) {
		return siteProps.getBoolean(CACHE_USE_ENTRY_LISTENER, true);
	}

}
