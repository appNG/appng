/*
 * Copyright 2011-2017 the original author or authors.
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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Platform;
import org.appng.api.model.Nameable;
import org.appng.api.model.Properties;
import org.appng.api.model.Resource;
import org.appng.api.model.Resources;
import org.appng.api.model.Site;
import org.appng.core.service.InitializerService;
import org.appng.tools.os.Command;
import org.appng.tools.os.OperatingSystem;
import org.appng.tools.os.StringConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides caching directories to the appNG platform and to appNG applications. Both types of caches provide a
 * dedicated directory layout per site and application.
 * 
 * The platform cache caches application-specific {@link Resource}s. Resources should be cached by calling
 * {@link Resources#dumpToCache(org.appng.api.model.ResourceType...)}.
 * 
 * The application cache may be used by applications and provides a location in the file system where instance-specific
 * files can be cached. These are usually artifacts which are based on some source data stored in a database.
 * 
 * The cache is not a persistent data storage and must not be used for unrecoverable data.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CacheProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(CacheProvider.class);
	private static final String WEB_INF = "WEB-INF";

	private final File platformRoot;
	private final String cacheFolder;
	private final String platformCacheFolder;
	private final String applicationCacheFolder;
	private final String imageCacheFolder;
	private final File webInf;
	private final File cache;
	private final File platform;
	private final File application;
	private final int prefixLength;

	/**
	 * Creates a new CacheProvider. Retrieves the directory layout from the platform configuration.
	 * 
	 * @param platformConfig
	 *            The platform configuration. Contains values for keys defined in
	 *            {@link org.appng.api.Platform.Property}.
	 */
	public CacheProvider(Properties platformConfig) {
		this(platformConfig, false);
	}

	/**
	 * Creates a new CacheProvider. Retrieves the directory layout from the platform configuration.
	 * 
	 * @param platformConfig
	 *            The platform configuration. Contains values for keys defined in
	 *            {@link org.appng.api.Platform.Property}.
	 * @param changeOwner
	 *            if set to {@code true} and we're running on *nix, a recursive {@code chown} command is being executed
	 *            for the cache folder
	 */
	public CacheProvider(Properties platformConfig, boolean changeOwner) {

		this.platformRoot = new File(platformConfig.getString(Platform.Property.PLATFORM_ROOT_PATH));
		prefixLength = this.platformRoot.getAbsolutePath().length();

		cacheFolder = platformConfig.getString(Platform.Property.CACHE_FOLDER);
		platformCacheFolder = platformConfig.getString(Platform.Property.PLATFORM_CACHE_FOLDER);
		applicationCacheFolder = platformConfig.getString(Platform.Property.APPLICATION_CACHE_FOLDER);
		imageCacheFolder = platformConfig.getString(Platform.Property.IMAGE_CACHE_FOLDER);

		webInf = new File(this.platformRoot, WEB_INF);
		cache = new File(webInf, cacheFolder);
		platform = new File(cache, platformCacheFolder);
		application = new File(cache, applicationCacheFolder);

		String appngUser = platformConfig.getString(InitializerService.APPNG_USER);
		String appngGroup = platformConfig.getString(InitializerService.APPNG_GROUP);
		if (changeOwner && OperatingSystem.isLinux() && StringUtils.isNoneBlank(appngUser, appngGroup)) {
			String command = String.format("chown -R %s:%s %s", appngUser, appngGroup, cache.getAbsolutePath());
			StringConsumer errorConsumer = new StringConsumer();
			if (0 != Command.execute(command, null, errorConsumer)) {
				LOGGER.warn("'{}' returned '{}'", command, StringUtils.join(errorConsumer.getResult(), "\r\n"));
			}
		}
	}

	/**
	 * Clears both the platform cache and the application cache for the given site.
	 * 
	 * @param site
	 *            The site which caches are cleared.
	 * 
	 */
	public void clearCache(Site site) {
		clear(getPlatformCache(site));
		clear(getApplicationCache(site));
	}

	/**
	 * Clears both the platform cache and the application cache for the specified application of the specified site.
	 * 
	 * @param site
	 * @param application
	 *            The application which caches are cleared. The application must be assigned to the aforementioned site.
	 *            The caches of the same application assigned to other sites will not be cleared.
	 */
	public void clearCache(Site site, String application) {
		clear(getPlatformCache(site.getName(), application));
		clear(getApplicationCache(site.getName(), application));
	}

	private void clear(File folder) {
		try {
			FileUtils.deleteDirectory(folder);
			LOGGER.info("cleaning " + folder.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.error("error while clearing cache for site " + folder.getName(), e);
		}
	}

	/**
	 * Returns the overall cache root directory.
	 * 
	 * @return the overall cache root directory
	 */
	protected File getCache() {
		return mkdir(cache);
	}

	/**
	 * Returns the root directory of the platform cache.
	 * 
	 * @return the root directory of the platform cache
	 */
	protected File getPlatformCache() {
		return mkdir(platform);
	}

	/**
	 * Returns the root platform cache directory for a site.
	 * 
	 * @param site
	 * @return the root platform cache directory for a site
	 */
	protected File getPlatformCache(Nameable site) {
		return mkdir(getPlatformCache(site.getName()));
	}

	/**
	 * Returns the root platform cache directory for a site.
	 * 
	 * @param site
	 * @return the root platform cache directory for a site
	 */
	protected File getPlatformCache(String site) {
		return mkdir(new File(getPlatformCache(), site));
	}

	/**
	 * Returns the root platform cache directory for an application.
	 * 
	 * @param site
	 * @param application
	 * @return the root platform cache directory for an application
	 */
	public File getPlatformCache(Nameable site, Nameable application) {
		return mkdir(getPlatformCache(site.getName(), application.getName()));
	}

	/**
	 * Returns the root platform cache directory for an application.
	 * 
	 * @param site
	 * @param application
	 * @return the root platform cache directory for an application
	 */
	protected File getPlatformCache(String site, String application) {
		return mkdir(new File(getPlatformCache(site), application));
	}

	/**
	 * Returns the relative platform cache directory for an application.
	 * 
	 * @param site
	 * @param application
	 * @return the relative platform cache directory for an application
	 */
	public String getRelativePlatformCache(Nameable site, Nameable application) {
		String applicationCache = getPlatformCache(site.getName(), application.getName()).getAbsolutePath();
		return applicationCache.substring(prefixLength);
	}

	/**
	 * Returns the root directory of the application cache.
	 * 
	 * @return the root directory of the application cache
	 */
	protected File getApplicationCache() {
		return mkdir(application);
	}

	/**
	 * Returns the root application cache directory for a site.
	 * 
	 * @param site
	 * @return the root application cache directory for a site
	 */
	protected File getApplicationCache(Nameable site) {
		return mkdir(getApplicationCache(site.getName()));
	}

	/**
	 * Returns the root application cache directory for a site.
	 * 
	 * @param site
	 * @return the root application cache directory for a site
	 */
	protected File getApplicationCache(String site) {
		return mkdir(new File(getApplicationCache(), site));
	}

	/**
	 * Returns the root application cache directory for an application.
	 * 
	 * @param site
	 * @param application
	 * @return the root application cache directory for an application
	 */
	protected File getApplicationCache(Nameable site, Nameable application) {
		return mkdir(getApplicationCache(site.getName(), application.getName()));
	}

	/**
	 * Returns the root application cache directory for an application.
	 * 
	 * @param site
	 * @param application
	 * @return the root application cache directory for an application
	 */
	protected File getApplicationCache(String site, String application) {
		return mkdir(new File(getApplicationCache(site), application));
	}

	/**
	 * Returns the root image cache directory for an application. The image cache directory is located within the
	 * application cache directory.
	 * 
	 * @param site
	 * @param application
	 * @return the root image cache directory for an application
	 */
	public File getImageCache(Nameable site, Nameable application) {
		return mkdir(getImageCache(site.getName(), application.getName()));
	}

	/**
	 * Returns the root image cache directory for an application. The image cache directory is located within the
	 * application cache directory.
	 * 
	 * @param site
	 * @param application
	 * @return the root image cache directory for an application
	 */
	protected File getImageCache(String site, String application) {
		return mkdir(new File(getApplicationCache(site, application), imageCacheFolder));
	}

	private File mkdir(File file) {
		try {
			FileUtils.forceMkdir(file);
		} catch (IOException e) {
			LOGGER.error("error while creating directory", e);
		}
		return file;
	}

}
