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
package org.appng.core.service.cache;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;

import org.appng.core.controller.CachedResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheEntryListener
		implements CacheEntryCreatedListener<String, CachedResponse>, CacheEntryRemovedListener<String, CachedResponse>,
		CacheEntryExpiredListener<String, CachedResponse>, Serializable {

	private final Set<String> keys = Collections.synchronizedSet(new HashSet<>());

	@Override
	public void onCreated(Iterable<CacheEntryEvent<? extends String, ? extends CachedResponse>> events)
			throws CacheEntryListenerException {
		events.forEach(e -> {
			keys.add(e.getKey());
			doLog("Added", e);
		});

	}

	@Override
	public void onExpired(Iterable<CacheEntryEvent<? extends String, ? extends CachedResponse>> events)
			throws CacheEntryListenerException {
		events.forEach(e -> {
			keys.remove(e.getKey());
			doLog("Expired", e);
		});
	}

	@Override
	public void onRemoved(Iterable<CacheEntryEvent<? extends String, ? extends CachedResponse>> events)
			throws CacheEntryListenerException {
		events.forEach(e -> {
			keys.remove(e.getKey());
			doLog("Removed", e);
		});
	}

	private void doLog(String verb, CacheEntryEvent<? extends String, ? extends CachedResponse> e) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(verb + ": " + e.getKey() + " (size: " + keys.size() + ")");
		}
	}

	public void clear() {
		keys.clear();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Cleared! (size: " + keys.size() + ")");
		}
	}

	public Set<String> getKeys() {
		return new HashSet<>(keys);
	}

	public Set<String> getKeys(String prefix) {
		return getKeys().parallelStream().filter(k -> k.startsWith(prefix)).collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		return "managing " + keys.size() + " keys";
	}
}