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
package org.appng.core.model;

import java.util.Collection;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.ApplicationProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link CacheManager} that is aware of {@link Site}s and {@link Application}s to avoid naming conflicts and also
 * ensure no other {@link Application} can access the {@link Cache}.<br/>
 * The TTL and maxIdle for the {@link Cache} can be configured using the application property
 * {@value ApplicationProperties#PROP_CACHE_CONFIG}.
 * 
 * @see ApplicationProperties#PROP_CACHE_CONFIG
 */
@Slf4j
public class ApplicationCacheManager implements CacheManager, DisposableBean {

	private static final String SUFFIX_TTL = ".ttl";
	private static final String SUFFIX_MAX_IDLE = ".maxIdle";
	private CacheManager delegate;
	private String prefix;
	private Properties cacheConfig;

	public void initialize(Site site, Application application, CacheManager delegate) {
		this.prefix = String.format("%s#%s_%s_", site.getName(), ((SiteImpl) site).getReloadCount(),
				application.getName());
		this.delegate = delegate;
		this.cacheConfig = application.getProperties().getProperties(ApplicationProperties.PROP_CACHE_CONFIG);
	}

	public Cache getCache(String name) {
		HazelcastInstance hazelcastInstance = ((HazelcastCacheManager) delegate).getHazelcastInstance();
		String nameInternal = prefix + name;
		Object ttl = null == cacheConfig ? null : cacheConfig.get(name + SUFFIX_TTL);
		MapConfig mapConfig = hazelcastInstance.getConfig().getMapConfigOrNull(nameInternal);
		if (null == mapConfig && null != ttl) {
			mapConfig = hazelcastInstance.getConfig().getMapConfig(nameInternal);
			mapConfig.setTimeToLiveSeconds(Integer.valueOf(String.valueOf(ttl)));
			Object maxIdle = cacheConfig.get(name + SUFFIX_MAX_IDLE);
			if (null != maxIdle) {
				mapConfig.setMaxIdleSeconds(Integer.valueOf(String.valueOf(maxIdle)));
			}
			LOGGER.info("Configured cache '{}' with TTL of {}s (max idle: {}s)", nameInternal,
					mapConfig.getTimeToLiveSeconds(), mapConfig.getMaxIdleSeconds());
		}

		return delegate.getCache(nameInternal);
	}

	public Collection<String> getCacheNames() {
		return getInternalCacheNames(n -> n.substring(prefix.length())).stream().collect(Collectors.toList());
	}

	Collection<String> getInternalCacheNames(Function<String, String> mapper) {
		Stream<String> stream = delegate.getCacheNames().stream().filter(n -> n.startsWith(prefix));
		if (null != mapper) {
			stream = stream.map(mapper);
		}
		return stream.collect(Collectors.toList());
	}

	public void destroy() throws Exception {
		getInternalCacheNames(null).stream().forEach(c -> {
			IMap<?, ?> cache = (IMap<?, ?>) delegate.getCache(c).getNativeCache();
			int size = cache.size();
			cache.destroy();
			LOGGER.info("destroyed cache '{}' with {} elements", c, size);
		});
	}
}
