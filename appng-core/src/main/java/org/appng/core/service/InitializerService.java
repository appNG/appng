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
package org.appng.core.service;

import static org.appng.api.support.environment.EnvironmentKeys.JAR_INFO_MAP;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.ApplicationController;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Application;
import org.appng.api.model.ApplicationSubject;
import org.appng.api.model.Group;
import org.appng.api.model.Named;
import org.appng.api.model.Properties;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.ApplicationConfigProviderImpl;
import org.appng.api.support.ConfigValidator;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.PropertyHolder;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.RepositoryWatcher;
import org.appng.core.controller.messaging.ReloadSiteEvent;
import org.appng.core.controller.messaging.SiteStateEvent;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.Template;
import org.appng.core.model.ApplicationContext;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.model.CacheProvider;
import org.appng.core.model.FeatureProviderImpl;
import org.appng.core.model.JarInfo;
import org.appng.core.model.JarInfo.JarInfoBuilder;
import org.appng.core.model.PlatformTransformer;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.repository.config.ApplicationPostProcessor;
import org.appng.search.indexer.DocumentIndexer;
import org.appng.tools.ui.StringNormalizer;
import org.appng.xml.MarshallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.blocking.BlockingCache;

/**
 * A service responsible for initializing the appNG platform with all active {@link Site}s.
 * 
 * @author Matthias Müller
 */
public class InitializerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(InitializerService.class);
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
	private TemplateService templateService;

	@Autowired
	private DatabaseService databaseService;

	@Autowired
	private MarshallService marshallService;

	/**
	 * Initializes and loads the platform, which includes logging some environment settings.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param rootConnection
	 *            the root {@link DatabaseConnection}
	 * @param ctx
	 *            the current {@link ServletContext}
	 * @param executor
	 *            an {@link ExecutorService} used by the cluster messaging
	 * @throws InvalidConfigurationException
	 *             if an configuration error occurred
	 * @see #loadPlatform(java.util.Properties, Environment, String, String, ExecutorService)
	 */
	public void initPlatform(java.util.Properties defaultOverrides, Environment env, DatabaseConnection rootConnection,
			ServletContext ctx, ExecutorService executor) throws InvalidConfigurationException {
		logEnvironment();
		loadPlatform(defaultOverrides, env, null, null, executor);
		addJarInfo(env, ctx);
		databaseService.setActiveConnection(rootConnection, false);
	}

	/**
	 * Reloads the platform with all of it's {@link Site}s.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param siteName
	 *            the (optional) name of the {@link Site} that caused the platform reload
	 * @param target
	 *            an (optional) target to redirect to after platform reload
	 * @throws InvalidConfigurationException
	 *             if an configuration error occurred
	 */
	public void reloadPlatform(java.util.Properties config, Environment env, String siteName, String target,
			ExecutorService executor) throws InvalidConfigurationException {
		LOGGER.info(StringUtils.leftPad("Reloading appNG", 100, "="));
		loadPlatform(config, env, siteName, target, executor);
		LOGGER.info(StringUtils.leftPad("appNG reloaded", 100, "="));
	}

	public InitializerService() {
		this.siteThreads = new ConcurrentHashMap<String, List<ExecutorService>>();
	}

	private void startIndexThread(Site site, DocumentIndexer documentIndexer) {
		startSiteThread(site, "appng-indexthread-" + site.getName(), THREAD_PRIORITY_LOW, documentIndexer);
	}

	private void startRepositoryWatcher(Site site, boolean ehcacheEnabled, String jspType) {
		if (ehcacheEnabled && site.getProperties().getBoolean(SiteProperties.EHCACHE_WATCH_REPOSITORY, false)) {
			String watcherRuleSourceSuffix = site.getProperties().getString(
					SiteProperties.EHCACHE_WATCHER_RULE_SOURCE_SUFFIX, RepositoryWatcher.DEFAULT_RULE_SUFFIX);
			String threadName = String.format("appng-repositoryWatcher-%s", site.getName());
			RepositoryWatcher repositoryWatcher = new RepositoryWatcher(site, jspType, watcherRuleSourceSuffix);
			startSiteThread(site, threadName, THREAD_PRIORITY_LOW, repositoryWatcher);
		}
	}

	private void startSiteThread(Site site, String threadName, int priority, Runnable runnable) {
		if (!siteThreads.containsKey(site.getName())) {
			siteThreads.put(site.getName(), new ArrayList<ExecutorService>());
		}
		ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setPriority(priority)
				.setNameFormat(threadName).build();
		ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
		siteThreads.get(site.getName()).add(executor);
		executor.execute(runnable);
		LOGGER.info("starting {}", threadName);
	}

	/**
	 * Loads the platform by loading every active {@link Site}.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param siteName
	 *            the (optional) name of the {@link Site} that caused the platform reload
	 * @param target
	 *            an (optional) target to redirect to after platform reload
	 * @throws InvalidConfigurationException
	 *             if an configuration error occurred
	 */
	public void loadPlatform(java.util.Properties defaultOverrides, Environment env, String siteName, String target,
			ExecutorService executor) throws InvalidConfigurationException {
		ServletContext servletContext = ((DefaultEnvironment) env).getServletContext();
		String rootPath = servletContext.getRealPath("/");
		PropertyHolder platformConfig = getCoreService().initPlatformConfig(defaultOverrides, rootPath, false, true,
				false);
		addPropertyIfExists(platformConfig, defaultOverrides, APPNG_USER);
		addPropertyIfExists(platformConfig, defaultOverrides, APPNG_GROUP);
		platformConfig.setFinal();

		RepositoryCacheFactory.init(platformConfig);

		String ehcacheConfig = platformConfig.getString(Platform.Property.EHCACHE_CONFIG);
		CacheManager cacheManager = CacheManager.create(rootPath + "/" + ehcacheConfig);

		String uploadDir = platformConfig.getString(Platform.Property.UPLOAD_DIR);
		String realPath = ((DefaultEnvironment) env).getServletContext().getRealPath(appendSlash(uploadDir));
		File tempDir = new File(realPath);
		if (!tempDir.exists()) {
			try {
				FileUtils.forceMkdir(tempDir);
			} catch (IOException e) {
				LOGGER.error("unable to create upload dir " + tempDir, e);
			}
		}

		env.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, platformConfig);
		Messaging.createMessageSender(env, executor);

		String applicationDir = platformConfig.getString(Platform.Property.APPLICATION_DIR);
		String applicationRealDir = servletContext.getRealPath(appendSlash(applicationDir));
		File applicationRootFolder = new File(applicationRealDir).getAbsoluteFile();
		if (!applicationRootFolder.exists()) {
			LOGGER.error("could not find applicationfolder " + applicationRootFolder.getAbsolutePath(),
					" platform will exit");
			return;
		}
		LOGGER.info("applications are located at " + applicationRootFolder + " or in the database");
		List<Integer> sites = getCoreService().getSiteIds();
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		if (null == siteMap) {
			siteMap = new ConcurrentHashMap<>();
			env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);
		}
		int activeSites = 0;
		for (Integer id : sites) {
			SiteImpl site = getCoreService().getSite(id);
			if (site.isActive()) {
				LOGGER.info(StringUtils.leftPad("", 90, "="));
				loadSite(site, env, false, new FieldProcessorImpl("load-platform"));
				activeSites++;
				LOGGER.info(StringUtils.leftPad("", 90, "="));
			} else {
				String runningSite = site.getName();
				site.setState(SiteState.INACTIVE);
				if (siteMap.containsKey(runningSite)) {
					getCoreService().shutdownSite(env, runningSite);
				} else {
					getCoreService().setSiteStartUpTime(site, null);
				}
				LOGGER.info("site {} is inactive and will not be loaded", site);
			}
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

		if (0 == activeSites) {
			LOGGER.error("none of " + sites.size() + " sites is active, instance will not work!");
		}
		LOGGER.info("Current Ehcache configuration:\n" + cacheManager.getActiveConfigurationText());

		if (null != siteName && null != target) {
			RequestUtil.getSiteByName(env, siteName).sendRedirect(env, target);
		}
	}

	private void addPropertyIfExists(PropertyHolder platformConfig, java.util.Properties defaultOverrides,
			String name) {
		if (defaultOverrides.containsKey(name)) {
			platformConfig.addProperty(name, defaultOverrides.getProperty(name), null);
		}
	}

	private String appendSlash(String value) {
		return value.startsWith(Path.SEPARATOR) ? value : Path.SEPARATOR + value;
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
					? StringNormalizer.replaceNonPrintableCharacters((String) value, qm) : value;
			LOGGER.info("{}: {}", StringNormalizer.replaceNonPrintableCharacters(key, qm), logValue);
		}
	}

	/**
	 * Initialized and returns the platform configuration represented by a {@link Properties} object.
	 * 
	 * @param rootPath
	 *            the root path of the platform (see {@link org.appng.api.Platform.Property#PLATFORM_ROOT_PATH})
	 * @param devMode
	 *            value for the {@link org.appng.api.Platform.Property#DEV_MODE} property to set
	 * @return the platform configuration
	 */
	public Properties initPlatformConfig(java.util.Properties defaultOverrides, String rootPath, Boolean devMode) {
		return getCoreService().initPlatformConfig(defaultOverrides, rootPath, devMode, false, true);
	}

	/**
	 * Loads the given {@link Site}.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param siteToLoad
	 *            the {@link Site} to load
	 * @throws InvalidConfigurationException
	 *             if an configuration error occurred
	 */
	public synchronized void loadSite(Environment env, SiteImpl siteToLoad, FieldProcessor fp)
			throws InvalidConfigurationException {
		loadSite(env, siteToLoad, true, fp);
	}

	/**
	 * Loads the given {@link Site}.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param siteToLoad
	 *            the {@link Site} to load
	 * @throws InvalidConfigurationException
	 *             if an configuration error occurred
	 */
	public synchronized void loadSite(Environment env, SiteImpl siteToLoad, boolean sendReloadEvent, FieldProcessor fp)
			throws InvalidConfigurationException {
		loadSite(siteToLoad, env, sendReloadEvent, fp);
	}

	/**
	 * Loads the given {@link Site}.
	 * 
	 * @param siteToLoad
	 *            the {@link Site} to load
	 * @param servletContext
	 *            the current {@link ServletContext}
	 * @throws InvalidConfigurationException
	 *             if an configuration error occurred
	 */
	public synchronized void loadSite(SiteImpl siteToLoad, ServletContext servletContext, FieldProcessor fp)
			throws InvalidConfigurationException {
		loadSite(siteToLoad, new DefaultEnvironment(servletContext, siteToLoad.getHost()), true, fp);
	}

	/**
	 * Loads the given {@link Site}.
	 * 
	 * @param siteToLoad
	 *            the {@link Site} to load, freshly loaded with {@link CoreService#getSite(Integer)} or
	 *            {@link CoreService#getSiteByName(String)}
	 * @param env
	 *            the current {@link Environment}
	 * @param sendReloadEvent
	 *            whether or not a {@link ReloadSiteEvent} should be sent
	 * @param fp
	 *            a {@link FieldProcessor} to attach messages to
	 * @throws InvalidConfigurationException
	 *             if an configuration error occurred
	 */
	public synchronized void loadSite(SiteImpl siteToLoad, Environment env, boolean sendReloadEvent, FieldProcessor fp)
			throws InvalidConfigurationException {
		ServletContext servletContext = ((DefaultEnvironment) env).getServletContext();
		Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);

		SiteImpl site = siteToLoad;
		Site currentSite = siteMap.get(site.getName());
		if (null != currentSite) {
			LOGGER.info("prepare reload of site {}, shutting down first", currentSite);
			shutDownSite(env, currentSite);
		}

		Sender sender = env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER);
		site.setSender(sender);
		List<? extends Group> groups = getCoreService().getGroups();
		site.setGroups(new HashSet<Named<Integer>>(groups));

		site.setState(SiteState.STARTING);
		siteMap.put(site.getName(), site);

		Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String repositoryDir = platformConfig.getString(Platform.Property.REPOSITORY_PATH);
		String repositoryRealDir = servletContext.getRealPath(repositoryDir);
		File siteRootDirectory = new File(repositoryRealDir, site.getName());
		site.setRootDirectory(siteRootDirectory);

		String host = site.getHost();
		org.springframework.context.ApplicationContext platformContext = env.getAttribute(Scope.PLATFORM,
				Platform.Environment.CORE_PLATFORM_CONTEXT);

		debugPlatformContext(platformContext);

		LOGGER.info("loading site " + site.getName() + " (" + host + ")");
		LOGGER.info("loading applications for site " + site.getName());
		List<URL> classPath = new ArrayList<URL>();

		Set<ApplicationProvider> applications = new HashSet<ApplicationProvider>();

		// platform and application cache
		CacheProvider cacheProvider = new CacheProvider(platformConfig, true);
		cacheProvider.clearCache(site);

		// ehcache
		Integer ehcacheBlockingTimeout = site.getProperties().getInteger(SiteProperties.EHCACHE_BLOCKING_TIMEOUT);
		BlockingCache cache = CacheService.getBlockingCache(site, ehcacheBlockingTimeout);
		Boolean ehcacheEnabled = site.getProperties().getBoolean(SiteProperties.EHCACHE_ENABLED);
		cache.setDisabled(!ehcacheEnabled);
		Boolean ehcacheStatistics = site.getProperties().getBoolean(SiteProperties.EHCACHE_STATISTICS);
		cache.setStatisticsEnabled(ehcacheStatistics);

		Properties siteProps = site.getProperties();
		String siteRoot = siteProps.getString(SiteProperties.SITE_ROOT_DIR);
		String indexdir = siteProps.getString(SiteProperties.INDEX_DIR);
		Integer indexQueueSize = siteProps.getInteger(SiteProperties.INDEX_QUEUE_SIZE);
		Long indexTimeout = siteProps.getInteger(SiteProperties.INDEX_TIMEOUT).longValue();
		DocumentIndexer documentIndexer = new DocumentIndexer(indexQueueSize, new File(siteRoot, indexdir),
				indexTimeout);

		Boolean devMode = platformConfig.getBoolean(Platform.Property.DEV_MODE);
		Boolean monitorPerformance = platformConfig.getBoolean(Platform.Property.MONITOR_PERFORMANCE);
		String applicationDir = platformConfig.getString(Platform.Property.APPLICATION_DIR);
		String rootPath = platformConfig.getString(Platform.Property.PLATFORM_ROOT_PATH);

		File applicationRootFolder = new File(rootPath, applicationDir).getAbsoluteFile();
		File imageMagickPath = new File(platformConfig.getString(Platform.Property.IMAGEMAGICK_PATH));

		String templateFolder = platformConfig.getString(Platform.Property.TEMPLATE_FOLDER);
		Template template = templateService.getTemplateByDisplayName(siteProps.getString(SiteProperties.TEMPLATE));
		if (null == template) {
			String templateRealPath = servletContext.getRealPath(templateFolder);
			TemplateService.copyTemplate(platformConfig, siteProps, templateRealPath);
		} else {
			TemplateService.materializeTemplate(template, platformConfig, siteProps);
		}

		// Step 1: Load applications for the current site,
		// prepare for further initialization
		for (SiteApplication siteApplication : site.getSiteApplications()) {
			if (siteApplication.isMarkedForDeletion()) {
				coreService.unlinkApplicationFromSite(site.getId(), siteApplication.getApplication().getId());
			} else if (!siteApplication.isActive()) {
				String message = String.format("%s:  %s is inactive.", site.getName(),
						siteApplication.getApplication().getName());
				LOGGER.info(message);
				fp.addNoticeMessage(message);
			} else {
				if (siteApplication.isReloadRequired()) {
					coreService.unsetReloadRequired(siteApplication);
				}
				Application application = siteApplication.getApplication();
				String errorMessage = String.format("Error while loading application %s.", application.getName());
				try {
					DatabaseConnection databaseConnection = siteApplication.getDatabaseConnection();
					if (null != databaseConnection) {
						databaseConnection.setActive(databaseConnection.testConnection(null));
						databaseConnection.setValidationPeriod(
								platformConfig.getInteger(Platform.Property.DATABASE_VALIDATION_PERIOD));
						databaseService.save(databaseConnection);
						if (!databaseConnection.isActive()) {
							throw new InvalidConfigurationException(site, application.getName(),
									String.format("Connection %s for application %s of site %s is not working!",
											databaseConnection, application.getName(), site.getName()));
						}
					}

					File applicationCacheFolder = cacheProvider.getPlatformCache(site, application);

					Resources applicationResources = getCoreService().getResources(application, applicationCacheFolder,
							applicationRootFolder);

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

					applicationResources.dumpToCache(ResourceType.BEANS_XML);
					applicationResources.dumpToCache(ResourceType.JAR);
					applicationResources.dumpToCache(ResourceType.SQL);
					applicationResources.dumpToCache(ResourceType.DICTIONARY);
					applicationResources.dumpToCache(ResourceType.RESOURCE);
					applicationResources.dumpToCache(ResourceType.TPL);
					if (devMode) {
						applicationResources.dumpToCache(ResourceType.XSL, ResourceType.XML);
					}

					ApplicationConfigProvider applicationConfig = new ApplicationConfigProviderImpl(marshallService,
							application.getName(), applicationResources, devMode);
					applicationProvider.setApplicationConfig(applicationConfig);

					Collection<Resource> messageSources = applicationResources.getResources(ResourceType.DICTIONARY);
					if (messageSources.size() > 0) {
						File cachedFile = messageSources.iterator().next().getCachedFile();
						URL messagesFolder = cachedFile.getParentFile().toURI().toURL();
						classPath.add(messagesFolder);
					}

					Collection<Resource> jars = applicationResources.getResources(ResourceType.JAR);
					for (Resource applicationResource : jars) {
						classPath.add(applicationResource.getCachedFile().toURI().toURL());
					}

					FeatureProviderImpl featureProvider = new FeatureProviderImpl(applicationProvider.getProperties());
					featureProvider.initImageProcessor(imageMagickPath, cacheProvider.getImageCache(site, application));
					featureProvider.setIndexer(documentIndexer);
					applicationProvider.setFeatureProvider(featureProvider);

					applications.add(applicationProvider);

					File sqlFolder = new File(applicationCacheFolder, ResourceType.SQL.getFolder());
					databaseService.migrateApplication(sqlFolder, siteApplication.getDatabaseConnection());

				} catch (MalformedURLException mfu) {
					fp.addErrorMessage(errorMessage);
					LOGGER.error(errorMessage, mfu);
				} catch (InvalidConfigurationException ice) {
					fp.addErrorMessage(errorMessage);
					LOGGER.error(errorMessage, ice);
				}
			}
		}

		site.getSiteApplications().clear();

		URL[] urls = classPath.toArray(new URL[classPath.size()]);
		ClassLoader classLoader = getClass().getClassLoader();
		SiteClassLoader siteClassLoader = new SiteClassLoader(urls, classLoader, site.getName());

		Thread.currentThread().setContextClassLoader(siteClassLoader);

		LOGGER.info(siteClassLoader.toString());
		site.setSiteClassLoader(siteClassLoader);

		startIndexThread(site, documentIndexer);
		startRepositoryWatcher(site, ehcacheEnabled, platformConfig.getString(Platform.Property.JSP_FILE_TYPE));

		String datasourceConfigurerName = siteProps.getString(SiteProperties.DATASOURCE_CONFIGURER);
		try {
			siteClassLoader.loadClass(datasourceConfigurerName);
		} catch (ClassNotFoundException e) {
			throw new InvalidConfigurationException(site, null,
					"error while loading class " + datasourceConfigurerName);
		}

		// Step 2: Build application context
		Set<ApplicationProvider> validApplications = new HashSet<ApplicationProvider>();
		for (ApplicationProvider application : applications) {
			try {
				String beansXmlLocation = cacheProvider.getRelativePlatformCache(site, application) + File.separator
						+ ResourceType.BEANS_XML_NAME;
				// this is required to support testing of InitialiterService
				List<String> configLocations = new ArrayList<String>(
						siteProps.getList(CONFIG_LOCATIONS, ApplicationContext.CONTEXT_CLASSPATH, ","));
				configLocations.add(beansXmlLocation);
				ApplicationContext applicationContext = new ApplicationContext(application, platformContext,
						site.getSiteClassLoader(), servletContext,
						configLocations.toArray(new String[configLocations.size()]));

				Set<Resource> resources = application.getResources().getResources(ResourceType.DICTIONARY);
				List<String> dictionaryNames = new ArrayList<String>();
				for (Resource applicationResource : resources) {
					String name = FilenameUtils.getBaseName(applicationResource.getName()).replaceAll("_(.)*", "");
					if (!dictionaryNames.contains(name)) {
						dictionaryNames.add(name);
					}
				}

				java.util.Properties props = PropertySupport.getProperties(platformConfig, site, application,
						application.isPrivileged());

				PropertySourcesPlaceholderConfigurer configurer = getPlaceholderConfigurer(props);
				applicationContext.addBeanFactoryPostProcessor(configurer);
				ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
				Properties appProps = application.getProperties();
				List<String> profiles = appProps.getList(ApplicationProperties.PROP_ACTIVE_PROFILES, ",");
				if (!profiles.isEmpty()) {
					environment.setActiveProfiles(profiles.toArray(new String[profiles.size()]));
				}
				environment.getPropertySources().addFirst(new PropertiesPropertySource("appngEnvironment", props));

				ApplicationPostProcessor applicationPostProcessor = new ApplicationPostProcessor(
						application.getDatabaseConnection(), dictionaryNames);
				applicationContext.addBeanFactoryPostProcessor(applicationPostProcessor);

				applicationContext.refresh();

				application.setContext(applicationContext);
				ConfigValidator configValidator = new ConfigValidator(application.getApplicationConfig());
				configValidator.validateMetaData(siteClassLoader);
				configValidator.validate(application.getName());
				configValidator.processErrors(application.getName());

				Collection<ApplicationSubject> applicationSubjects = coreService
						.getApplicationSubjects(application.getId(), site);
				application.getApplicationSubjects().addAll(applicationSubjects);
				validApplications.add(application);
			} catch (Exception e) {
				String message = String.format("Error while loading application %s.", application.getName());
				fp.addErrorMessage(message);
				LOGGER.error(message, e);
			}
		}
		site.getSiteApplications().clear();
		site.getSiteApplications().addAll(validApplications);

		List<JarInfo> jarInfos = new ArrayList<JarInfo>();

		// Step 3: Execute application-specific initialization,
		// read JAR info, cleanup on errors
		for (ApplicationProvider application : validApplications) {
			if (startApplication(env, site, application)) {
				jarInfos.addAll(application.getJarInfos());
				LOGGER.info("Initialized application: " + application.getName());
				for (JarInfo jarInfo : application.getJarInfos()) {
					LOGGER.info(jarInfo.toString());
				}
			}
		}

		env.setAttribute(Scope.PLATFORM, site.getName() + "." + EnvironmentKeys.JAR_INFO_MAP, jarInfos);

		PlatformTransformer.clearCache();
		coreService.setSiteStartUpTime(site, new Date());
		if (sendReloadEvent) {
			site.sendEvent(new ReloadSiteEvent(site.getName()));
		}
		LOGGER.info("loading site " + site.getName() + " completed");
		site.setState(SiteState.STARTED);
		siteMap.put(site.getName(), site);
		debugPlatformContext(platformContext);
	}

	protected boolean startApplication(Environment env, SiteImpl site, ApplicationProvider application) {
		boolean started = true;
		ApplicationController controller = application.getBean(ApplicationController.class);
		if (null != controller) {
			try {
				started = controller.start(site, application, env);
			} catch (RuntimeException e) {
				LOGGER.error("error during " + controller.getClass().getName() + ".start()", e);
				started = false;
			}
			if (!started) {
				LOGGER.error(
						"Failed to initialize application: " + application.getName() + ", so it will be shut down.");
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
	 * @see #shutDownSite(Environment, Site)
	 */
	public void shutdownPlatform(ServletContext ctx) {
		Environment env = DefaultEnvironment.get(ctx);
		Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		if (null == siteMap) {
			LOGGER.info("no sites found, must be boot sequence");
		} else {
			LOGGER.debug("destroying platform");
			Set<String> siteNames = new HashSet<String>(siteMap.keySet());
			for (String siteName : siteNames) {
				Site site = siteMap.get(siteName);
				shutDownSite(env, site);
			}
		}
		CacheManager.getInstance().shutdown();
		env.removeAttribute(Scope.PLATFORM, Platform.Environment.SITES);
	}

	/**
	 * Shuts down the given {@link Site}.
	 * 
	 * @param env
	 *            the current {@link Environment}.
	 * @param site
	 *            the {@link Site} to shut down
	 */
	public void shutDownSite(Environment env, Site site) {
		List<ExecutorService> executors = siteThreads.get(site.getName());
		LOGGER.info("shutting down site threads for {}", site);
		for (ExecutorService executorService : executors) {
			executorService.shutdownNow();
		}
		coreService.shutdownSite(env, site.getName());
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

			List<JarInfo> jarInfos = new ArrayList<JarInfo>();
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

}
