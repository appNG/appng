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
package org.appng.core.controller;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.SiteProperties;
import org.appng.api.XPathProcessor;
import org.appng.api.model.Site;
import org.appng.core.controller.filter.RedirectFilter;
import org.appng.core.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import net.sf.ehcache.Ehcache;

/**
 * <p>
 * A service that watches for modified/deleted files in a {@link Site}'s www-directory (see
 * {@link SiteProperties#WWW_DIR}) using a {@link WatchService}.
 * </p>
 * If caching for the site is active (see {@link SiteProperties#EHCACHE_ENABLED}), cache entries for the
 * modified/deleted files are removed from the cache. Since there could be some forwarding rules defined in the site's
 * {@code urlrewrite.xml}, it is also necessary to parse these rules and remove the 'aliases' from the cache.
 * 
 * @author Matthias Müller
 *
 */
public class RepositoryWatcher implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryWatcher.class);
	private static final String XPATH_FORWARD_RULE = "//rule[not(/to/@type) or /to/@type = 'forward']";
	public static final String DEFAULT_RULE_SUFFIX = "((\\?\\S+)?)";
	private String jspExtension = ".jsp";

	private WatchService watcher;
	private boolean needsToBeWatched = false;
	private Map<String, List<String>> forwardMap;

	private String wwwDir;

	private Ehcache cache;

	private File configFile;

	private String ruleSourceSuffix;

	public RepositoryWatcher(Site site, String jspExtension, String ruleSourceSuffix) {
		try {
			this.jspExtension = "." + jspExtension;
			String rootDir = site.getProperties().getString(SiteProperties.SITE_ROOT_DIR);
			String wwwdir = site.getProperties().getString(SiteProperties.WWW_DIR);
			Ehcache cache = CacheService.getBlockingCache(site);
			String rewriteConfig = site.getProperties().getString(SiteProperties.REWRITE_CONFIG);
			List<String> documentsDirs = site.getProperties().getList(SiteProperties.DOCUMENT_DIR, ";");
			init(cache, rootDir + wwwdir, site.readFile(rewriteConfig), ruleSourceSuffix, documentsDirs);
		} catch (Exception e) {
			LOG.error(String.format("error starting RepositoryWatcher for site %s", site.getName()), e);
		}
	}

	RepositoryWatcher() {

	}

	void init(Ehcache cache, String wwwDir, File configFile, String ruleSourceSuffix, List<String> documentDirs)
			throws Exception {
		this.cache = cache;
		this.watcher = FileSystems.getDefault().newWatchService();
		this.wwwDir = FilenameUtils.normalize(wwwDir, true);
		this.configFile = configFile;
		this.ruleSourceSuffix = ruleSourceSuffix;
		readUrlRewrites(configFile);

		for (String docDir : documentDirs) {
			Path path = new File(wwwDir, docDir).toPath();
			LOG.info("watching {}", path.toString());
			path.register(watcher, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		}

	}

	public void run() {
		LOG.info("start watching...");
		for (;;) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				long start = System.currentTimeMillis();
				if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}
				Path eventPath = (Path) key.watchable();
				File absoluteFile = new File(eventPath.toFile(), ((Path) event.context()).toString());
				if (absoluteFile.equals(configFile)) {
					readUrlRewrites(absoluteFile);
				}
				LOG.debug("received event {} for {}", event.kind(), absoluteFile);
				String absolutePath = FilenameUtils.normalize(absoluteFile.getPath(), true);
				String relativePathName = absolutePath.substring(wwwDir.length());
				if (relativePathName.endsWith(jspExtension)) {
					relativePathName = relativePathName.substring(0, relativePathName.length() - jspExtension.length());
				}
				removeFromCache(relativePathName);
				if (forwardMap.containsKey(relativePathName)) {
					forwardMap.get(relativePathName).forEach(path -> removeFromCache(path));
				}
				LOG.debug("processed event {} for {} ins {}ms", event.kind(), relativePathName,
						System.currentTimeMillis() - start);
			}
			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}

	}

	private int removeFromCache(String relativePathName) {
		int count = 0;
		@SuppressWarnings("unchecked")
		List<String> keys = cache.getKeys();
		for (String cacheKey : keys) {
			if (cacheKey.startsWith(HttpMethod.GET.name() + relativePathName)) {
				if (cache.remove(cacheKey)) {
					LOG.debug("removed from cache: {}", cacheKey);
					count++;
				}
			}
		}
		LOG.info("removed {} cache elements for {} (cache size: {})", count, relativePathName, keys.size());
		return count;
	}

	public boolean needsToBeWatched() {
		return needsToBeWatched;
	}

	private void readUrlRewrites(File configFile) {
		forwardMap = new HashMap<>();
		if (configFile.exists()) {
			try {
				Document parseconfigFile = RedirectFilter.parseConfig(configFile.toURI().toURL());
				XPathProcessor xPathProcessor = new XPathProcessor(parseconfigFile);
				NodeList forwardRules = xPathProcessor.getNodes(XPATH_FORWARD_RULE);
				for (int i = 0; i < forwardRules.getLength(); i++) {
					org.w3c.dom.Element rule = (org.w3c.dom.Element) forwardRules.item(i);
					String from = rule.getElementsByTagName("from").item(0).getTextContent();
					from = from.replace("^", StringUtils.EMPTY).replace("$", StringUtils.EMPTY);
					from = from.replace(ruleSourceSuffix, StringUtils.EMPTY);
					String to = rule.getElementsByTagName("to").item(0).getTextContent();
					if (to.contains(jspExtension)) {
						to = to.substring(0, to.indexOf(jspExtension));
					}
					if (!forwardMap.containsKey(to)) {
						forwardMap.put(to, new ArrayList<>());
					}
					forwardMap.get(to).add(from);
				}
				LOG.info("{} has been read, {} forward rules have been processed", configFile.getAbsolutePath(),
						forwardRules.getLength());
			} catch (Exception e) {
				LOG.error(String.format("error reading %s", configFile.getAbsolutePath()), e);
			}
		} else {
			LOG.info("config file for reading rewrite rules does not exist: {}", configFile.getAbsolutePath());
		}
	}

}
