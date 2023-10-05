/*
 * Copyright 2011-2021 the original author or authors.
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

import static org.appng.api.support.environment.EnvironmentKeys.JAR_INFO_MAP;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.ApplicationController;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Application;
import org.appng.api.model.ApplicationSubject;
import org.appng.api.model.Group;
import org.appng.api.model.Properties;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.ApplicationConfigProviderImpl;
import org.appng.api.support.ConfigValidator;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.RepositoryWatcher;
import org.appng.core.controller.handler.GuiHandler;
import org.appng.core.controller.messaging.ReloadSiteEvent;
import org.appng.core.controller.rest.RestPostProcessor;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.domain.PlatformEventListener;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.ApplicationContext;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.model.CacheProvider;
import org.appng.core.model.FeatureProviderImpl;
import org.appng.core.model.JarInfo;
import org.appng.core.model.JarInfo.JarInfoBuilder;
import org.appng.core.model.PlatformTransformer;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.repository.config.ApplicationPostProcessor;
import org.appng.core.service.MigrationService.MigrationStatus;
import org.appng.search.indexer.DocumentIndexer;
import org.appng.tools.ui.StringNormalizer;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Messages;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * A service responsible for initializing the appNG platform with all active {@link Site}s.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class InitializerService {

	private static final int THREAD_PRIORITY_LOW = 3;

	private static final String LIB_LOCATION = "/WEB-INF/lib";
	private static final String EXT_JAR = ".jar";
	private static final String CONFIG_LOCATIONS = "configLocations";
	public static final String APPNG_USER = "appng.user";
	public static final String APPNG_GROUP = "appng.group";

	private ConcurrentMap<String, List<ExecutorService>> siteThreads;

	@Autowired
	private CoreService coreService;

	@Autowired
	private DatabaseService databaseService;

	@Autowired
	private MarshallService marshallService;

	@Autowired
	protected PlatformEventListener auditableListener;

	@Transactional
	@Deprecated
	public void initPlatform(PlatformProperties platformConfig, Environment env, DatabaseConnection rootConnection,
			ServletContext ctx, ExecutorService executor) throws InvalidConfigurationException {
		initPlatform(platformConfig, env, rootConnection, ctx, executor, null);
	}

	/**
	 * Initializes and loads the platform, which includes logging some environment settings.
	 * 
	 * @param platformConfig
	 *                          the current {@link PlatformProperties}
	 * @param env
	 *                          the current {@link Environment}
	 * @param rootConnection
	 *                          the root {@link DatabaseConnection}
	 * @param ctx
	 *                          the current {@link ServletContext}
	 * @param messagingExecutor
	 *                          an {@link ExecutorService} used for cluster communication threads
	 * @param startupExecutor
	 *                          an {@link ExecutorService} used for starting sites in parallel
	 * 
	 * @throws InvalidConfigurationException
	 *                                       if an configuration error occurred
	 * 
	 * @see #loadPlatform(PlatformProperties, Environment, String, String, ExecutorService)
	 */
	@Transactional
	public void initPlatform(PlatformProperties platformConfig, Environment env, DatabaseConnection rootConnection,
			ServletContext ctx, ExecutorService messagingExecutor, ExecutorService startupExecutor)
			throws InvalidConfigurationException {
		logEnvironment();
		loadPlatform(platformConfig, env, null, null, messagingExecutor, startupExecutor);
		addJarInfo(env, ctx);
		databaseService.setActiveConnection(rootConnection, false);
		coreService.createEvent(Type.INFO, "Started platform");
	}


	/**
	 * @param config
	 * @param env
	 * @param siteName
	 * @param target
	 * @param messagingExecutor
	 * 
	 * @deprecated will be removed with no replacement
	 * 
	 * @throws InvalidConfigurationException
	 */
	@Deprecated
	public void reloadPlatform(java.util.Properties config, Environment env, String siteName, String target,
			ExecutorService messagingExecutor) throws InvalidConfigurationException {
		throw new UnsupportedOperationException();
	}

	public InitializerService() {
		this.siteThreads = new ConcurrentHashMap<>();
	}

	private void startIndexThread(Site site, DocumentIndexer documentIndexer) {
		startSiteThread(site, "appng-indexthread-" + site.getName(), THREAD_PRIORITY_LOW, documentIndexer);
	}

	private void startRepositoryWatcher(Site site, boolean cacheEnabled, String jspType) {
		if (cacheEnabled && site.getProperties().getBoolean(SiteProperties.CACHE_WATCH_REPOSITORY, false)) {
			String watcherRuleSourceSuffix = site.getProperties()
					.getString(SiteProperties.CACHE_WATCHER_RULE_SOURCE_SUFFIX, RepositoryWatcher.DEFAULT_RULE_SUFFIX);
			String threadName = String.format("appng-repositoryWatcher-%s", site.getName());
			RepositoryWatcher repositoryWatcher = new RepositoryWatcher(site, jspType, watcherRuleSourceSuffix);
			startSiteThread(site, threadName, THREAD_PRIORITY_LOW, repositoryWatcher);
		}
	}

	private void startSiteThread(Site site, String threadName, int priority, Runnable runnable) {
		if (!siteThreads.containsKey(site.getName())) {
			siteThreads.put(site.getName(), new ArrayList<>());
		}
		ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setPriority(priority)
				.setNameFormat(threadName).build();
		ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
		siteThreads.get(site.getName()).add(executor);
		executor.execute(runnable);
		LOGGER.info("started site thread [{}] with runnable of type {}", threadName, runnable.getClass().getName());
	}

	@Deprecated
	public void loadPlatform(PlatformProperties platformConfig, Environment env, String siteName, String target,
			ExecutorService messagingExecutor) throws InvalidConfigurationException {
		loadPlatform(platformConfig, env, siteName, target, messagingExecutor, null);
	}

	/**
	 * Loads the platform by loading every active {@link Site}.
	 * 
	 * @param platformConfig
	 *                          the current {@link PlatformProperties}
	 * @param env
	 *                          the current {@link Environment}
	 * @param siteName
	 *                          the (optional) name of the {@link Site} that caused the platform reload
	 * @param target
	 *                          an (optional) target to redirect to after platform reload
	 * @param messagingExecutor
	 *                          an {@link ExecutorService} used for cluster communication threads
	 * @param startupExecutor
	 *                          an {@link ExecutorService} used for starting sites in parallel
	 * 
	 * @throws InvalidConfigurationException
	 *                                       if an configuration error occurred
	 */
	public void loadPlatform(PlatformProperties platformConfig, Environment env, String siteName, String target,
			ExecutorService messagingExecutor, ExecutorService startupExecutor) throws InvalidConfigurationException {

		if (platformConfig.getBoolean(Platform.Property.CLEAN_TEMP_FOLDER_ON_STARTUP, true)) {
			File tempDir = new File(System.getProperty("java.io.tmpdir"));
			if (tempDir.exists()) {
				LOGGER.info("Cleaning temp folder {}", tempDir);
				try {
					FileUtils.cleanDirectory(tempDir);
				} catch (IOException e) {
					LOGGER.error(String.format("error while cleaning %s", tempDir), e);
				}
			}
		}

		RepositoryCacheFactory.init(platformConfig);

		HazelcastInstance hazelcast = HazelcastConfigurer.getInstance(platformConfig, Messaging.getNodeId(env));
		CacheService.createCacheManager(hazelcast, HazelcastConfigurer.isClient());
		HazelcastInstance hazelcastInstance = ((HazelcastCacheManager) CacheService.getCacheManager())
				.getHazelcastInstance();
		LOGGER.info("Caching uses {}", hazelcastInstance);

		File uploadDir = platformConfig.getUploadDir();
		if (!uploadDir.exists()) {
			try {
				FileUtils.forceMkdir(uploadDir);
			} catch (IOException e) {
				LOGGER.error(String.format("unable to create upload dir %s", uploadDir), e);
			}
		}

		Sender sender = Messaging.createMessageSender(env, messagingExecutor);

		File applicationRootFolder = platformConfig.getApplicationDir();
		if (!applicationRootFolder.exists()) {
			LOGGER.error("could not find applicationfolder {} platform will exit!",
					applicationRootFolder.getAbsolutePath());
			return;
		}
		LOGGER.info("applications are located at {} or in the database", applicationRootFolder);
		Map<String, Site> siteMap = new ConcurrentHashMap<>();
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);

		final int heartBeatSleepTime = platformConfig.getInteger(Platform.Property.HEART_BEAT_INTERVAL, 60) * 1000;
		if (null != sender) {
			new HeartBeat(heartBeatSleepTime, ((DefaultEnvironment) env).getServletContext()).start();
		}

		int activeSites = 0;
		FieldProcessor platformMessages = new FieldProcessorImpl("load-platform");
		env.setAttribute(Scope.PLATFORM, GuiHandler.PLATFORM_MESSAGES, platformMessages.getMessages());
		List<Integer> sites = getCoreService().getSiteIds();
		Boolean parallelSiteStarts = platformConfig.getBoolean(Platform.Property.PARALLEL_SITE_STARTS, false);
		for (Integer id : sites) {
			try {
				SiteImpl site = getCoreService().getSite(id);
				if (site.isActive()) {
					Runnable siteLoader = getSiteLoader(site, env, false, platformMessages, parallelSiteStarts);
					if (parallelSiteStarts) {
						startupExecutor.execute(siteLoader);
					} else {
						LOGGER.info(StringUtils.leftPad("", 90, "="));
						siteLoader.run();
						LOGGER.info(StringUtils.leftPad("", 90, "="));
					}
					activeSites++;
				} else {
					String inactiveSite = site.getName();
					site.setState(SiteState.INACTIVE, env);
					if (siteMap.containsKey(inactiveSite)) {
						getCoreService().shutdownSite(env, inactiveSite, false);
					} else {
						siteMap.put(inactiveSite, site);
						getCoreService().setSiteStartUpTime(site, null);
					}
					LOGGER.info("site {} is inactive and will not be loaded", site);
				}
			} catch (Throwable e) {
				LOGGER.error("Failed loading site", e);
			}
		}

		if (0 == activeSites) {
			LOGGER.error("none of {} sites is active, instance will not work!", sites.size());
		}

		if (null != siteName && null != target) {
			RequestUtil.getSiteByName(env, siteName).sendRedirect(env, target);
		}
	}

	public PlatformProperties loadPlatformProperties(java.util.Properties defaultOverrides, Environment env) {
		ServletContext servletContext = ((DefaultEnvironment) env).getServletContext();
		String rootPath = servletContext.getRealPath("/");
		PlatformProperties platformConfig = getCoreService().initPlatformConfig(defaultOverrides, rootPath, false, true,
				false);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, platformConfig);
		return platformConfig;
	}

	public Properties loadNodeProperties(Environment env) {
		Properties nodeConfig = getCoreService().initNodeConfig(env);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.NODE_CONFIG, nodeConfig);
		return nodeConfig;
	}

	class SiteReloadWatcher implements Runnable {

		private static final String RELOAD_FILE = ".reload";
		private Environment env;
		private Site site;

		public SiteReloadWatcher(Environment env, Site site) {
			this.env = env;
			this.site = site;
		}

		public void run() {
			try {
				try (WatchService watcher = FileSystems.getDefault().newWatchService()) {

					File rootDir = new File(site.getProperties().getString(SiteProperties.SITE_ROOT_DIR));
					rootDir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
							StandardWatchEventKinds.ENTRY_MODIFY);
					LOGGER.debug("watching for {}", new File(rootDir, RELOAD_FILE).getAbsolutePath());

					File absoluteFile = null;
					do {
						WatchKey key;
						try {
							key = watcher.take();
						} catch (InterruptedException x) {
							Thread.currentThread().interrupt();
							return;
						}
						for (WatchEvent<?> event : key.pollEvents()) {
							if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
								continue;
							}
							java.nio.file.Path eventPath = (java.nio.file.Path) key.watchable();
							String fileName = ((java.nio.file.Path) event.context()).toString();
							if (RELOAD_FILE.equals(fileName)) {
								absoluteFile = new File(eventPath.toFile(), fileName);
								LOGGER.info("found {}", absoluteFile.getAbsolutePath());
							}
						}
					} while (null == absoluteFile);

					FileUtils.deleteQuietly(absoluteFile);
					LOGGER.info("deleted {}", absoluteFile.getAbsolutePath());
					LOGGER.info("restarting site {}", site.getName());
					try {
						FieldProcessor reloadMessages = new FieldProcessorImpl("auto-reload");
						loadSite(env, getCoreService().getSiteByName(site.getName()), false, reloadMessages);
						Messages platformMessages = reloadMessages.getMessages();
						env.setAttribute(Scope.PLATFORM, GuiHandler.PLATFORM_MESSAGES, platformMessages);
					} catch (InvalidConfigurationException e) {
						LOGGER.error(String.format("error while reloading site %s", site.getName()), e);
					}
				}
			} catch (Exception e) {
				LOGGER.error("error in site reload watcher", e);
			}
			LOGGER.info("done watching for reload file.");
		}

	}

	private void logHeaderMessage(String message) {
		String separator = StringUtils.leftPad(StringUtils.EMPTY, 15, "-");
		LOGGER.info(separator + StringUtils.SPACE + message + StringUtils.SPACE + separator);
	}

	private void logEnvironment() {
		if (LOGGER.isInfoEnabled()) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Map<String, Object> properties = new HashMap(System.getProperties());
			logMap(properties, "System Properties");
			logMap(System.getenv(), "System Environment");
			LOGGER.info(StringUtils.leftPad("", 90, "="));
		}
	}

	private void logMap(Map<String, ?> map, String headline) {
		List<String> keyList = Arrays.asList(map.keySet().toArray(new String[map.size()]));
		Collections.sort(keyList);
		logHeaderMessage(headline);
		String qm = "?";
		for (String key : keyList) {
			Object value = map.get(key);
			Object logValue = (value instanceof String)
					? StringNormalizer.replaceNonPrintableCharacters((String) value, qm)
					: value;
			LOGGER.info("{}: {}", StringNormalizer.replaceNonPrintableCharacters(key, qm), logValue);
		}
	}

	/**
	 * Loads the given {@link Site}.
	 * 
	 * @param env
	 *                   the current {@link Environment}
	 * @param siteToLoad
	 *                   the {@link Site} to load
	 * 
	 * @throws InvalidConfigurationException
	 *                                       if an configuration error occurred
	 */
	@Transactional
	public synchronized void loadSite(Environment env, SiteImpl siteToLoad, FieldProcessor fp)
			throws InvalidConfigurationException {
		loadSite(env, siteToLoad, true, fp);
	}

	/**
	 * Loads the given {@link Site}.
	 * 
	 * @param env
	 *                   the current {@link Environment}
	 * @param siteToLoad
	 *                   the {@link Site} to load
	 * 
	 * @throws InvalidConfigurationException
	 *                                       if an configuration error occurred
	 */
	@Transactional
	public synchronized void loadSite(Environment env, SiteImpl siteToLoad, boolean sendReloadEvent, FieldProcessor fp)
			throws InvalidConfigurationException {
		loadSite(siteToLoad, env, sendReloadEvent, fp, false);
	}

	/**
	 * Loads the given {@link Site}.
	 * 
	 * @param siteToLoad
	 *                       the {@link Site} to load
	 * @param servletContext
	 *                       the current {@link ServletContext}
	 * 
	 * @throws InvalidConfigurationException
	 *                                       if an configuration error occurred
	 */
	public synchronized void loadSite(SiteImpl siteToLoad, ServletContext servletContext, FieldProcessor fp)
			throws InvalidConfigurationException {
		loadSite(siteToLoad, DefaultEnvironment.get(servletContext), true, fp, false);
	}

	public synchronized void loadSite(SiteImpl siteToLoad, Environment env, boolean sendReloadEvent, FieldProcessor fp,
			boolean setThreadName) throws InvalidConfigurationException {
		getSiteLoader(siteToLoad, env, sendReloadEvent, fp, setThreadName).run();
	}

	/**
	 * Returns a {@link Runnable} that loads the given {@link Site}.
	 * 
	 * @param siteToLoad
	 *                        the {@link Site} to load, freshly loaded with {@link CoreService#getSite(Integer)} or
	 *                        {@link CoreService#getSiteByName(String)}
	 * @param env
	 *                        the current {@link Environment}
	 * @param sendReloadEvent
	 *                        whether or not a {@link ReloadSiteEvent} should be sent
	 * @param fp
	 *                        a {@link FieldProcessor} to attach messages to
	 * @param setThreadName
	 * 
	 * @return the {@link Runnable}
	 */
	public Runnable getSiteLoader(SiteImpl siteToLoad, Environment env, boolean sendReloadEvent, FieldProcessor fp,
			boolean setThreadName) {
		return () -> {
			StopWatch sw = new StopWatch("Loading site " + siteToLoad.getName());
			sw.start("Setup");
			if (setThreadName) {
				Thread.currentThread().setName("siteloader-" + siteToLoad.getName());
			}
			SiteImpl site = siteToLoad;
			try {
				ServletContext servletContext = ((DefaultEnvironment) env).getServletContext();
				Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);

				Site currentSite = siteMap.get(site.getName());
				boolean isReload = null != currentSite;
				if (isReload) {
					LOGGER.info("prepare reload of site {}, shutting down first", currentSite);
					shutDownSite(env, currentSite, false);
					site.setReloadCount(site.getReloadCount() + 1);
				}

				Sender sender = env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER);
				if (null == sender && Messaging.isEnabled(env)) {
					LOGGER.warn("Failed to retrieve {} although messaging is enabled!", Sender.class.getName());
				}
				site.setSender(sender);
				List<? extends Group> groups = coreService.getGroups();
				site.setGroups(new HashSet<>(groups));

				site.setState(SiteState.STARTING, env);
				siteMap.put(site.getName(), site);

				File siteRootDirectory = new File(site.getProperties().getString(SiteProperties.SITE_ROOT_DIR));
				site.setRootDirectory(siteRootDirectory);

				String host = site.getHost();
				org.springframework.context.ApplicationContext platformContext = env.getAttribute(Scope.PLATFORM,
						Platform.Environment.CORE_PLATFORM_CONTEXT);

				debugPlatformContext(platformContext);

				LOGGER.info("loading site {} ({})", site.getName(), host);
				LOGGER.info("loading applications for site {}", site.getName());

				SiteClassLoaderBuilder siteClassPath = new SiteClassLoaderBuilder();
				Set<ApplicationProvider> applications = new HashSet<>();

				PlatformProperties platformConfig = PlatformProperties.get(env);
				// platform and application cache
				CacheProvider cacheProvider = new CacheProvider(platformConfig, true);
				cacheProvider.clearCache(site);

				// cache
				Boolean cacheEnabled = site.getProperties().getBoolean(SiteProperties.CACHE_ENABLED);
				if (cacheEnabled) {
					CacheService.createCache(site);
				}

				Properties siteProps = site.getProperties();
				String siteRoot = siteProps.getString(SiteProperties.SITE_ROOT_DIR);
				String indexdir = siteProps.getString(SiteProperties.INDEX_DIR);
				Integer indexQueueSize = siteProps.getInteger(SiteProperties.INDEX_QUEUE_SIZE);
				Long indexTimeout = siteProps.getInteger(SiteProperties.INDEX_TIMEOUT).longValue();
				DocumentIndexer documentIndexer = new DocumentIndexer(indexQueueSize, new File(siteRoot, indexdir),
						indexTimeout);

				Boolean devMode = platformConfig.getBoolean(Platform.Property.DEV_MODE);
				Boolean monitorPerformance = platformConfig.getBoolean(Platform.Property.MONITOR_PERFORMANCE);

				File applicationRootFolder = platformConfig.getApplicationDir();
				File imageMagickPath = new File(platformConfig.getString(Platform.Property.IMAGEMAGICK_PATH));

				coreService.refreshTemplate(site, platformConfig);
				Integer validationPeriod = platformConfig.getInteger(Platform.Property.DATABASE_VALIDATION_PERIOD);

				// Step 1: Load applications for the current site,
				// prepare for further initialization
				for (SiteApplication siteApplication : site.getSiteApplications()) {
					sw.stop();
					sw.start("Phase 1: Initialize application " + siteApplication.getApplication().getName());
					if (siteApplication.isMarkedForDeletion()) {
						coreService.unlinkApplicationFromSite(site.getId(), siteApplication.getApplication().getId());
					} else if (!siteApplication.isActive()) {
						String message = String.format("[%s] Application '%s' is inactive.", site.getName(),
								siteApplication.getApplication().getName());
						LOGGER.info(message);
						fp.addNoticeMessage(message);
					} else {
						if (siteApplication.isReloadRequired()) {
							coreService.unsetReloadRequired(siteApplication);
						}
						Application application = siteApplication.getApplication();

						try {
							DatabaseConnection databaseConnection = siteApplication.getDatabaseConnection();
							if (null != databaseConnection) {
								boolean isActive = databaseConnection.isActive();
								boolean isWorking = databaseConnection.testConnection(null);
								if (isWorking ^ isActive) {
									databaseConnection.setActive(isWorking);
									databaseService.save(databaseConnection);
									siteApplication = coreService.getSiteApplication(
											siteApplication.getSite().getName(),
											siteApplication.getApplication().getName());
									databaseConnection = siteApplication.getDatabaseConnection();
								}
								if (isWorking) {
									databaseConnection.setValidationPeriod(validationPeriod);
								} else {
									throw new InvalidConfigurationException(site, application.getName(),
											String.format("Connection %s for application %s of site %s is not working!",
													databaseConnection, application.getName(), site.getName()));
								}
							}

							File applicationCacheFolder = cacheProvider.getPlatformCache(site, application);

							Resources applicationResources = getCoreService().getResources(application,
									applicationCacheFolder, applicationRootFolder);

							Resource beanSource = applicationResources.getResource(ResourceType.BEANS_XML,
									ResourceType.BEANS_XML_NAME);
							if (null == beanSource) {
								throw new InvalidConfigurationException(site, application.getName(),
										String.format("application '%s' does not contain a resource named '%s'",
												application.getName(), ResourceType.BEANS_XML_NAME));
							}
							ApplicationProvider applicationProvider = new ApplicationProvider(site, application,
									monitorPerformance);
							getCoreService().initApplicationProperties(site, applicationProvider);
							applicationProvider.setResources(applicationResources);
							applicationProvider.setDatabaseConnection(siteApplication.getDatabaseConnection());

							List<ResourceType> resourceTypes = Arrays.asList(ResourceType.BEANS_XML, ResourceType.JAR,
									ResourceType.SQL, ResourceType.DICTIONARY, ResourceType.RESOURCE, ResourceType.TPL);
							if (devMode) {
								resourceTypes = new ArrayList<>(resourceTypes);
								resourceTypes.add(ResourceType.XSL);
								resourceTypes.add(ResourceType.XML);
							}
							applicationResources.dumpToCache(resourceTypes.toArray(new ResourceType[0]));

							ApplicationConfigProvider applicationConfig = new ApplicationConfigProviderImpl(
									marshallService, application.getName(), applicationResources, devMode);
							applicationProvider.setApplicationConfig(applicationConfig);

							Collection<Resource> messageSources = applicationResources
									.getResources(ResourceType.DICTIONARY);
							if (messageSources.size() > 0) {
								File cachedFile = messageSources.iterator().next().getCachedFile();
								siteClassPath.addFolder(cachedFile.getParentFile().toPath(), application.getName());
							}

							Collection<Resource> jars = applicationResources.getResources(ResourceType.JAR);
							for (Resource applicationResource : jars) {
								File cachedFile = applicationResource.getCachedFile();
								String origin = siteClassPath.addJar(cachedFile.toPath(), application.getName());
								if (!application.getName().equals(origin)) {
									LOGGER.warn(
											"{} from application {} has not been added to the site's classpath, since this jar has already been added by application {}",
											cachedFile.getName(), application.getName(), origin);
								}
							}

							FeatureProviderImpl featureProvider = new FeatureProviderImpl(
									applicationProvider.getProperties());
							featureProvider.initImageProcessor(imageMagickPath,
									cacheProvider.getImageCache(site, application));
							featureProvider.setIndexer(documentIndexer);
							applicationProvider.setFeatureProvider(featureProvider);

							applications.add(applicationProvider);

						} catch (InvalidConfigurationException ice) {
							String errorMessage = String.format("[%s] Error while loading application '%s'.",
									site.getName(), application.getName());
							fp.addErrorMessage(errorMessage);
							LOGGER.error(errorMessage, ice);
							auditableListener.createEvent(Type.ERROR, errorMessage);
						}
					}
				}

				site.getSiteApplications().clear();

				SiteClassLoader siteClassLoader = siteClassPath.build(getClass().getClassLoader(), site.getName());
				Thread.currentThread().setContextClassLoader(siteClassLoader);

				LOGGER.info(siteClassLoader.toString());
				site.setSiteClassLoader(siteClassLoader);
				if (LOGGER.isDebugEnabled()) {
					List<URL> urlList = Arrays.asList(siteClassLoader.getURLs());
					urlList.sort((a, b) -> StringUtils.compare(a.toString(), b.toString()));
					LOGGER.debug("Classloader for site {} contains the following URLs: {}", site.getName(),
							StringUtils.join(urlList, ','));
				}

				startIndexThread(site, documentIndexer);
				startRepositoryWatcher(site, cacheEnabled, platformConfig.getString(Platform.Property.JSP_FILE_TYPE));

				String datasourceConfigurerName = siteProps.getString(SiteProperties.DATASOURCE_CONFIGURER);
				siteClassLoader.loadClass(datasourceConfigurerName);

				org.springframework.cache.CacheManager platformCacheManager = platformContext
						.getBean(org.springframework.cache.CacheManager.class);

				// Step 2: Build application context
				String dataBasePrefix = platformConfig.getString(Platform.Property.DATABASE_PREFIX);
				Set<ApplicationProvider> validApplications = new HashSet<>();
				for (ApplicationProvider application : applications) {
					sw.stop();
					sw.start("Phase 2: Load application " + application.getName());
					try {
						File applicationCacheFolder = cacheProvider.getPlatformCache(site, application);
						File sqlFolder = new File(applicationCacheFolder, ResourceType.SQL.getFolder());
						SiteApplication siteApplication = coreService.getSiteApplication(site.getName(),
								application.getName());
						MigrationStatus migrationStatus = databaseService.migrateApplication(sqlFolder, application,
								dataBasePrefix);
						DatabaseConnection dbc = application.getDatabaseConnection();
						siteApplication.setDatabaseConnection(dbc);

						if (migrationStatus.isErroneous()) {
							String errorMessage = String.format(
									"[%s] Database '%s' for application '%s' is in an errorneous state, please check the connection and the migration state!",
									site.getName(), dbc.getDatabaseName(), application.getName());
							fp.addErrorMessage(errorMessage);
						}

						String beansXmlLocation = cacheProvider.getRelativePlatformCache(site, application)
								+ File.separator + ResourceType.BEANS_XML_NAME;
						// this is required to support testing of InitializerService
						List<String> configLocations = new ArrayList<>(
								siteProps.getList(CONFIG_LOCATIONS, ApplicationContext.CONTEXT_CLASSPATH, ","));
						configLocations.add(beansXmlLocation);
						ApplicationContext applicationContext = new ApplicationContext(application, platformContext,
								site.getSiteClassLoader(), servletContext,
								configLocations.toArray(new String[configLocations.size()]));

						Set<Resource> resources = application.getResources().getResources(ResourceType.DICTIONARY);
						List<String> dictionaryNames = new ArrayList<>();
						for (Resource applicationResource : resources) {
							String name = FilenameUtils.getBaseName(applicationResource.getName()).replaceAll("_(.)*",
									"");
							if (!dictionaryNames.contains(name)) {
								dictionaryNames.add(name);
							}
						}

						java.util.Properties props = PropertySupport.getProperties(platformConfig, site, application,
								application.isPrivileged());

						PropertySourcesPlaceholderConfigurer configurer = getPlaceholderConfigurer(props);
						applicationContext.addBeanFactoryPostProcessor(configurer);
						ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext
								.getEnvironment();
						Properties appProps = application.getProperties();
						List<String> profiles = appProps.getList(ApplicationProperties.PROP_ACTIVE_PROFILES, ",");
						if (!profiles.isEmpty()) {
							environment.setActiveProfiles(profiles.toArray(new String[profiles.size()]));
						}
						environment.getPropertySources()
								.addFirst(new PropertiesPropertySource("appngEnvironment", props));

						ApplicationPostProcessor applicationPostProcessor = new ApplicationPostProcessor(site,
								application, dbc, platformCacheManager, dictionaryNames);
						applicationContext.addBeanFactoryPostProcessor(applicationPostProcessor);

						applicationContext.addBeanFactoryPostProcessor(new RestPostProcessor());

						applicationContext.refresh();

						application.setContext(applicationContext);
						ConfigValidator configValidator = new ConfigValidator(application.getApplicationConfig());
						configValidator.validateMetaData(siteClassLoader);
						configValidator.validate(application.getName(), site.getSiteClassLoader());
						configValidator.processErrors(application.getName());

						Collection<ApplicationSubject> applicationSubjects = coreService
								.getApplicationSubjects(application.getId(), site);
						application.getApplicationSubjects().addAll(applicationSubjects);
						validApplications.add(application);
					} catch (Throwable e) {
						String message = String.format("[%s] Error while loading application '%s'.", site.getName(),
								application.getName());
						fp.addErrorMessage(message);
						LOGGER.error(message, e);
						auditableListener.createEvent(Type.ERROR, message);
					}
				}
				site.getSiteApplications().clear();
				site.getSiteApplications().addAll(validApplications);

				sw.stop();
				sw.start("Phase 3: Initialize applications");

				List<JarInfo> jarInfos = new ArrayList<>();

				// Step 3: Execute application-specific initialization,
				// read JAR info, cleanup on errors
				for (ApplicationProvider application : validApplications) {
					if (startApplication(env, site, application)) {
						jarInfos.addAll(application.getJarInfos());
						LOGGER.info("Initialized application: {}", application.getName());
						for (JarInfo jarInfo : application.getJarInfos()) {
							LOGGER.info(jarInfo.toString());
						}
					} else {
						String message = String.format("[%s] Error while starting application '%s'.", site.getName(),
								application.getName());
						fp.addErrorMessage(message);
						auditableListener.createEvent(Type.ERROR, message);
					}
				}

				env.setAttribute(Scope.PLATFORM, site.getName() + "." + EnvironmentKeys.JAR_INFO_MAP, jarInfos);

				PlatformTransformer.clearCache(site);
				coreService.setSiteStartUpTime(site, new Date());

				if (site.getProperties().getBoolean(SiteProperties.SUPPORT_RELOAD_FILE)) {
					startSiteThread(site, "appng-sitereload-" + site.getName(), THREAD_PRIORITY_LOW,
							new SiteReloadWatcher(env, site));
				}
				sw.stop();
				LOGGER.info("loading site {} completed in {}ms", site.getName(), sw.getTotalTimeMillis());
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(sw.prettyPrint());

				}
				site.setState(SiteState.STARTED, env);
				siteMap.put(site.getName(), site);
				debugPlatformContext(platformContext);
				auditableListener.createEvent(Type.INFO, "Loaded site " + site.getName());

				if (sendReloadEvent) {
					site.sendEvent(new ReloadSiteEvent(site.getName()));
					if (isReload) {
						getCoreService().setSiteReloadCount(site);
					}
				}
			} catch (Throwable t) {
				site.setState(SiteState.INACTIVE);
				throw new SiteLoadingException("Error while loading site " + siteToLoad.getName(), t);
			} finally {
				if (sw.isRunning()) {
					sw.stop();
				}
			}
		};
	}

	protected boolean startApplication(Environment env, SiteImpl site, ApplicationProvider application) {
		boolean started = true;
		ApplicationController controller = application.getBean(ApplicationController.class);
		Throwable startError = null;
		if (null != controller) {
			try {
				started = controller.start(site, application, env);
			} catch (Throwable e) {
				started = false;
				startError = e;
			}
			if (!started) {
				String message = String.format("Application %s for site %s failed to start, so it will be shut down.",
						site.getName(), application.getName());
				if (null == startError) {
					LOGGER.error(message);
				} else {
					LOGGER.error(message, startError);
				}
				controller.shutdown(site, application, env);
				site.getSiteApplications().remove(application);
				application.closeContext();
				application = null;
			}
		}
		return started;
	}

	@SuppressWarnings("rawtypes")
	private void debugPlatformContext(org.springframework.context.ApplicationContext platformContext) {
		if (LOGGER.isDebugEnabled()) {
			try {
				BeanFactory bf = ((ConfigurableApplicationContext) platformContext).getBeanFactory();
				Field aBNBT = DefaultListableBeanFactory.class.getDeclaredField("allBeanNamesByType");
				aBNBT.setAccessible(true);

				Field dBM = DefaultSingletonBeanRegistry.class.getDeclaredField("dependentBeanMap");
				dBM.setAccessible(true);
				LOGGER.debug("BeanFactory for context {} is {}#{}", platformContext, bf.getClass().getName(),
						bf.hashCode());
				LOGGER.debug("allBeanNamesByType: {} items", ((Map) aBNBT.get(bf)).size());
				LOGGER.debug("dependentBeanMap: {} items", ((Map) dBM.get(bf)).size());

			} catch (Exception e) {
				// ignore
			}
		}
	}

	protected PropertySourcesPlaceholderConfigurer getPlaceholderConfigurer(java.util.Properties props) {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setProperties(props);
		configurer.setOrder(0);
		return configurer;
	}

	/**
	 * Shuts down the whole platform by shutting down every active {@link Site}.
	 * 
	 * @param ctx
	 *            the current {@link ServletContext}
	 * 
	 * @see #shutDownSite(Environment, Site, boolean)
	 */
	public void shutdownPlatform(ServletContext ctx) {
		Environment env = DefaultEnvironment.get(ctx);
		Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		if (null == siteMap) {
			LOGGER.info("no sites found, must be boot sequence");
		} else {
			LOGGER.debug("destroying platform");
			Set<String> siteNames = new HashSet<>(siteMap.keySet());
			for (String siteName : siteNames) {
				Site site = siteMap.get(siteName);
				shutDownSite(env, site, true);
			}
		}
		CacheService.shutdown();
		env.removeAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		coreService.createEvent(Type.INFO, "Stopped platform");
	}

	/**
	 * Shuts down the given {@link Site}.
	 * 
	 * @param env
	 *             the current {@link Environment}.
	 * @param site
	 *             the {@link Site} to shut down
	 */
	public void shutDownSite(Environment env, Site site, boolean removeFromSiteMap) {
		List<ExecutorService> executors = siteThreads.get(site.getName());
		if (null != executors) {
			LOGGER.info("shutting down site threads for {}", site);
			for (ExecutorService executorService : executors) {
				executorService.shutdownNow();
			}
		}
		coreService.shutdownSite(env, site.getName(), removeFromSiteMap);
	}

	public CoreService getCoreService() {
		return coreService;
	}

	public void setCoreService(CoreService coreService) {
		this.coreService = coreService;
	}

	private void addJarInfo(Environment env, ServletContext ctx) {
		String libPath = ctx.getRealPath(LIB_LOCATION);
		File libFolder = new File(libPath);
		final File[] listFiles = libFolder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(EXT_JAR);
			}
		});

		if (null != listFiles) {

			List<File> jarFiles = Arrays.asList(listFiles);
			Collections.sort(jarFiles);

			List<JarInfo> jarInfos = new ArrayList<>();
			logHeaderMessage("JAR Libraries");
			for (File jarFile : jarFiles) {
				final JarInfo jarInfo = JarInfoBuilder.getJarInfo(jarFile);
				LOGGER.info(jarInfo.toString());
				jarInfos.add(jarInfo);
				if (jarInfo.getFileName().startsWith("appng-core")) {
					String appngVersion = jarInfo.getImplementationVersion();
					env.setAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION, appngVersion);
				}
			}
			env.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG + "." + JAR_INFO_MAP, jarInfos);
		}
	}

	static class SiteClassLoaderBuilder {
		private Map<java.nio.file.Path, String> paths = new HashMap<>();

		String addJar(java.nio.file.Path jarfile, String origin) {
			Optional<java.nio.file.Path> exisiting = paths.keySet().parallelStream()
					.filter(p -> p.getFileName().equals(jarfile.getFileName())).findFirst();
			if (exisiting.isPresent()) {
				return paths.get(exisiting.get());
			}
			paths.put(jarfile, origin);
			return origin;
		}

		void addFolder(java.nio.file.Path folder, String origin) {
			paths.put(folder, origin);
		}

		SiteClassLoader build(ClassLoader parent, String site) {
			List<URL> urls = paths.keySet().parallelStream().map(p -> {
				try {
					return p.toUri().toURL();
				} catch (MalformedURLException e) {
					LOGGER.warn(String.format("Error building SiteClassLoader for site %s", site), e);
				}
				return null;
			}).filter(u -> u != null).collect(Collectors.toList());
			return new SiteClassLoader(urls.toArray(new URL[0]), parent, site);
		}
	}
}
