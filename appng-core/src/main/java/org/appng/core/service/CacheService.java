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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.spi.CachingProvider;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.BusinessException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.core.controller.AppngCacheElement;
import org.appng.core.domain.SiteImpl;

import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.core.Hazelcast;
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

	/**
	 * Returns the {@link CacheManager} instance.
	 * 
	 * @return The {@link CacheManager} instance.
	 */
	public static CacheManager createCacheManager(Properties cachingProps) {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		if (null == cachingProps) {
			return cachingProvider.getCacheManager();
		} else {

			String mode = cachingProps.getProperty("mode", "server");
			String addresses = cachingProps.getProperty("addresses", "localhost:5701");
			String group = cachingProps.getProperty("group", "dev");
			String port = cachingProps.getProperty("port", "5701");
			String multicastGroup = cachingProps.getProperty("multicastGroup", MulticastConfig.DEFAULT_MULTICAST_GROUP);
			String multicastPort = cachingProps.getProperty("multicastPort",
					String.valueOf(MulticastConfig.DEFAULT_MULTICAST_PORT));
			String multicastTimeoutSeconds = cachingProps.getProperty("multicastTimeoutSeconds",
					String.valueOf(MulticastConfig.DEFAULT_MULTICAST_TIMEOUT_SECONDS));
			String multicastTimeToLive = cachingProps.getProperty("multicastTimeToLive",
					String.valueOf(MulticastConfig.DEFAULT_MULTICAST_TTL));

			HazelcastInstance instance;
			Config config = new Config();
			config.setInstanceName("appNG");
			config.getNetworkConfig().setPort(Integer.valueOf(port));
			JoinConfig joinConfig = config.getNetworkConfig().getJoin();
			switch (mode) {
			case "client":
				ClientConfig clientConfig = new ClientConfig();
				clientConfig.getGroupConfig().setName(group);
				String[] addressArr = addresses.split(",");
				for (String address : addressArr) {
					clientConfig.getNetworkConfig().addAddress(address.trim());
				}
				instance = HazelcastClient.newHazelcastClient(clientConfig);
				break;

			case "tcp":
				joinConfig.getTcpIpConfig().setEnabled(true);
				joinConfig.getMulticastConfig().setEnabled(false);
				joinConfig.getTcpIpConfig().addMember(addresses);
				instance = Hazelcast.getOrCreateHazelcastInstance(config);
				break;

			default:
				joinConfig.getTcpIpConfig().setEnabled(false);
				joinConfig.getMulticastConfig().setEnabled(true);
				joinConfig.getMulticastConfig().setMulticastGroup(multicastGroup);
				joinConfig.getMulticastConfig().setMulticastPort(Integer.valueOf(multicastPort));
				joinConfig.getMulticastConfig().setMulticastTimeoutSeconds(Integer.valueOf(multicastTimeoutSeconds));
				joinConfig.getMulticastConfig().setMulticastTimeToLive(Integer.valueOf(multicastTimeToLive));
				instance = Hazelcast.getOrCreateHazelcastInstance(config);
				break;
			}
			
			Properties properties = new Properties();
			properties.put(HazelcastCachingProvider.HAZELCAST_INSTANCE_ITSELF, instance);
			return cachingProvider.getCacheManager(null, null, properties);
		}
	}

	/**
	 * Returns the {@link CacheManager} instance.
	 * 
	 * @return The {@link CacheManager} instance.
	 */
	public static CacheManager getCacheManager() {
		return Caching.getCachingProvider().getCacheManager();
	}

	/**
	 * Returns the {@link Cache} instance for the selected {@link Site}. Use this
	 * method to retrieve a cache instance which already must exists.
	 * 
	 * @param site The {@link Site} to get the cache for
	 * @return The {@link Cache} instance for the specified site.
	 */
	public static Cache<String, AppngCacheElement> getCache(Site site) {
		return getCacheManager().getCache(getCacheKey(site));
	}

	public static void clearCache(Site site) {
		Cache<String, AppngCacheElement> cache = getCache(site);
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
	 * @param ttl               the ttl for a cache element
	 * @param statisticsEnabled
	 * @return The {@link Cache} instance for the specified site.
	 */
	public synchronized static Cache<String, AppngCacheElement> createCache(Site site, Integer ttl,
			Boolean statisticsEnabled) {
		String cacheKey = getCacheKey(site);
		CacheManager cacheManager = getCacheManager();
		Cache<String, AppngCacheElement> cache = cacheManager.getCache(cacheKey);
		if (null == cache) {
			MutableConfiguration<String, AppngCacheElement> configuration = new MutableConfiguration<>();
			Factory<ExpiryPolicy> expiryPolicy = AccessedExpiryPolicy
					.factoryOf(new Duration(TimeUnit.MILLISECONDS, ttl));
			configuration.setExpiryPolicyFactory(expiryPolicy);
			configuration.setStatisticsEnabled(statisticsEnabled);
			Factory<CacheEntryListener<String, AppngCacheElement>> listenerFactory = () -> new CacheElementListener(
					ttl);
			configuration.addCacheEntryListenerConfiguration(
					new MutableCacheEntryListenerConfiguration<>(listenerFactory, null, true, true));
			cache = cacheManager.createCache(cacheKey, configuration);
		}
		return cache;
	}

	static class CacheElementListener implements CacheEntryCreatedListener<String, AppngCacheElement>,
			CacheEntryUpdatedListener<String, AppngCacheElement> {

		private final Integer ttl;

		CacheElementListener(Integer ttl) {
			this.ttl = ttl;
		}

		public void onCreated(Iterable<CacheEntryEvent<? extends String, ? extends AppngCacheElement>> events)
				throws CacheEntryListenerException {
			for (CacheEntryEvent<? extends String, ? extends AppngCacheElement> cacheEntryEvent : events) {
				AppngCacheElement value = cacheEntryEvent.getValue();
				updateEntry(value, true);
			}

		}

		public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends AppngCacheElement>> events)
				throws CacheEntryListenerException {
			for (CacheEntryEvent<? extends String, ? extends AppngCacheElement> cacheEntryEvent : events) {
				AppngCacheElement value = cacheEntryEvent.getValue();
				updateEntry(value, false);
			}

		}

		private void updateEntry(AppngCacheElement value, boolean isNew) {
			Date now = new Date();
			if (isNew) {
				value.setCreationTime(now);
				value.setTimeToLive(ttl);
			} else {
				value.incrementHit();
			}
			value.setLastAccessedTime(now);
			value.setExpirationTime(DateUtils.addMilliseconds(now, ttl));
		}

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

	enum CacheStatistics {
		CacheHits, CacheHitPercentage, CacheMisses, CacheMissPercentage, CacheGets, CachePuts, CacheRemovals,
		CacheEvictions, AverageGetTime, AveragePutTime, AverageRemoveTime
	}

	public static Map<String, String> getCacheStatistics(SiteImpl site) {
		Map<String, String> stats = new HashMap<>();
		Boolean cacheEnabled = site.getProperties().getBoolean(SiteProperties.EHCACHE_ENABLED);
		if (cacheEnabled) {
			try {
				Cache<String, ?> cache = CacheService.getCache(site);
				ObjectName objectName = new ObjectName("javax.cache:type=CacheStatistics");
				MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				if (null != cache) {
					stats.put("Average get time", String
							.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.AverageGetTime.name())));
					stats.put("Average put time", String
							.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.AveragePutTime.name())));
					stats.put("Average removal time", String
							.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.AverageRemoveTime.name())));
					stats.put("Hits",
							String.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.CacheHits.name())));
					stats.put("Misses",
							String.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.CacheMisses.name())));
					stats.put("Name", cache.getName());
					stats.put("Hits (%)", String
							.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.CacheHitPercentage.name())));
					stats.put("Misses (%)", String
							.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.CacheMissPercentage.name())));
					stats.put("Gets",
							String.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.CacheGets.name())));
					stats.put("Puts",
							String.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.CachePuts.name())));
					stats.put("Removals",
							String.valueOf(mBeanServer.getAttribute(objectName, CacheStatistics.CacheRemovals.name())));
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

	public static List<AppngCacheElement> getCacheEntries(SiteImpl site) {
		List<AppngCacheElement> appngCacheEntries = new ArrayList<>();
		try {
			Cache<String, AppngCacheElement> cache = CacheService.getCache(site);
			if (null != cache) {
				for (javax.cache.Cache.Entry<String, AppngCacheElement> entry : cache) {
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
			Cache<String, AppngCacheElement> cache = CacheService.getCache(site);
			for (javax.cache.Cache.Entry<String, AppngCacheElement> entry : cache) {
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
