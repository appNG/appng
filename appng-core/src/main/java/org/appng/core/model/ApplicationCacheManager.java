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
package org.appng.core.model;

import java.util.Collection;
import java.util.stream.Collectors;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * A {@link CacheManager} that is aware of {@link Site}s and {@link Application}s to avoid naming conflicts and also
 * ensure no other {@link Application} can access the {@link Cache}.
 */
public class ApplicationCacheManager implements CacheManager {

	private CacheManager delegate;
	private String prefix;

	public void initialize(Site site, Application application, CacheManager delegate) {
		this.prefix = String.format("%s-%s-", application.getName(), site.getName());
		this.delegate = delegate;
	}

	public Cache getCache(String name) {
		return delegate.getCache(prefix + name);
	}

	public Collection<String> getCacheNames() {
		return delegate.getCacheNames().stream().map(n -> n.substring(prefix.length())).collect(Collectors.toList());
	}

}
