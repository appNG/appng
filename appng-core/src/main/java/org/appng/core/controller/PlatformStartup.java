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
package org.appng.core.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.messaging.ShutdownEvent;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.HazelcastConfigurer;
import org.appng.core.service.HsqlStarter;
import org.appng.core.service.InitializerService;
import org.appng.core.service.MigrationService;
import org.appng.core.service.PlatformProperties;
import org.appng.el.ExpressionEvaluator;
import org.hsqldb.Server;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StopWatch;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * This {@link ServletContextListener} is used to initialize the appNG platform. This includes loading the configuration
 * from {@value #CONFIG_LOCATION}, initializing the {@link ApplicationContext} from
 * {@code /WEB-INF/conf/platformContext.xml} and loading all active {@link Site}s with their configured
 * {@link Application}s.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class PlatformStartup implements ServletContextListener {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(sys|env)(\\.|\\[).*\\}");
	static final String APPNG_STARTED = "APPNG_STARTED";
	public static final String APPNG_CONTEXT = "appNG platform context";
	public static final String CONFIG_LOCATION = "/conf/appNG.properties";
	public static final String WEB_INF = "/WEB-INF";
	private ExecutorService messagingExecutor;

	public void contextInitialized(ServletContextEvent sce) {

		try {
			final StopWatch startupWatch = new StopWatch("startup");
			printLogo();
			startupWatch.start();
			ServletContext ctx = sce.getServletContext();
			String appngData = System.getProperty(Platform.Property.APPNG_DATA);

			InputStream configIs;
			if (StringUtils.isBlank(appngData)) {
				configIs = ctx.getResourceAsStream(WEB_INF + CONFIG_LOCATION);
			} else {
				configIs = new FileInputStream(Paths.get(appngData, CONFIG_LOCATION).toFile());
			}

			Environment env = DefaultEnvironment.initGlobal(ctx);

			Properties config = new Properties();
			config.load(configIs);
			configIs.close();
			applySystem(config);

			Server hsqlServer = HsqlStarter.startHsql(config, ctx.getRealPath(""));
			if (null != hsqlServer) {
				ctx.setAttribute(HsqlStarter.CONTEXT, hsqlServer);
			}

			DatabaseConnection platformConnection = new MigrationService().initDatabase(config);
			LOGGER.info("Platform connection: {}", platformConnection);
			String nodeId = Messaging.init();

			initPlatformContext(ctx, env, config, platformConnection);
			InitializerService service = getService(env);
			final PlatformProperties platformProperties = service.loadPlatformProperties(config, env);
			service.loadNodeProperties(env);
			File debugFolder = new File(appngData, "debug").getAbsoluteFile();
			if (!(debugFolder.exists() || debugFolder.mkdirs())) {
				LOGGER.warn("Failed to create debug folder at {}", debugFolder.getPath());
			}

			messagingExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
					new ThreadFactoryBuilder().setDaemon(true).setNameFormat("messaging-" + nodeId + "-%d").build());

			service.initPlatform(platformProperties, env, platformConnection, ctx, messagingExecutor);
			startupWatch.stop();
			String appngVersion = env.getAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION);
			LOGGER.info("appNG {} started in {} ms.", appngVersion, startupWatch.getTotalTimeMillis());
			LOGGER.info(StringUtils.leftPad("", 100, "="));
			ctx.setAttribute(APPNG_STARTED, true);

		} catch (Exception e) {
			LOGGER.error("error during platform startup", e);
			contextDestroyed(sce);
		}
	}

	public static void applySystem(Properties config) {
		Map<String, Object> params = new HashMap<>();
		params.put("env", System.getenv());
		params.put("sys", System.getProperties());
		ExpressionEvaluator ee = new ExpressionEvaluator(params);
		for (Entry<Object, Object> entry : config.entrySet()) {
			String value = (String) entry.getValue();
			if (value.contains("${")) {
				entry.setValue(ee.evaluate((String) value, String.class));
				if (LOGGER.isDebugEnabled()) {
					Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
					while (matcher.find()) {
						LOGGER.debug("Replacing {} with system provided value for key '{}'", matcher.group(),
								entry.getKey());
					}
				}
			}
		}
	}

	private void printLogo() throws URISyntaxException, IOException, FileNotFoundException {
		LOGGER.info(StringUtils.repeat("-", 48));
		String appNGVersion = "appNG.version";
		String jarPath = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		if (jarPath.endsWith(".jar")) {
			try (JarInputStream jis = new JarInputStream(new FileInputStream(jarPath))) {
				appNGVersion = jis.getManifest().getMainAttributes().getValue("Implementation-Version");
			}
		}
		try (InputStream logoIs = getClass().getResourceAsStream("logo.txt")) {
			IOUtils.readLines(logoIs, StandardCharsets.UTF_8).stream().forEach(LOGGER::info);
		}
		LOGGER.info(StringUtils.leftPad(appNGVersion, 41, "-") + "-------");
		LOGGER.info("...the Next Generation Application Platform");
	}

	protected void initPlatformContext(ServletContext ctx, Environment env, Properties config,
			DatabaseConnection platformConnection) throws IOException {
		AnnotationConfigWebApplicationContext platformCtx = new AnnotationConfigWebApplicationContext();
		platformCtx.register(PlatformConfig.class);
		platformCtx.setDisplayName(APPNG_CONTEXT);
		platformCtx.setServletContext(ctx);
		PropertySourcesPlaceholderConfigurer appNGConfigurer = new PropertySourcesPlaceholderConfigurer();
		config.put(DatabaseService.DATABASE_TYPE, platformConnection.getType().name());
		config.put(DatabaseService.DATABASE_MIN_CONNECTIONS, platformConnection.getMinConnections());
		config.put(DatabaseService.DATABASE_MAX_CONNECTIONS, platformConnection.getMaxConnections());
		config.put(DatabaseService.DATABASE_NAME, platformConnection.getName());
		appNGConfigurer.setProperties(config);
		platformCtx.addBeanFactoryPostProcessor(appNGConfigurer);
		platformCtx.refresh();
		ctx.setAttribute(Platform.Environment.CORE_PLATFORM_CONTEXT, platformCtx);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT, platformCtx);
	}

	public void contextDestroyed(ServletContextEvent sce) {
		LOGGER.info("Stopping appNG");
		ServletContext ctx = sce.getServletContext();
		DefaultEnvironment env = DefaultEnvironment.getGlobal();
		InitializerService initializerService = getService(env);
		if (null != initializerService) {
			initializerService.shutdownPlatform(ctx);
			ConfigurableApplicationContext platformCtx = env.removeAttribute(Scope.PLATFORM,
					Platform.Environment.CORE_PLATFORM_CONTEXT);
			org.apache.commons.logging.LogFactory.release(platformCtx.getClassLoader());
			platformCtx.close();
		}
		Optional.ofNullable(Messaging.getMessageSender(env)).ifPresent(s -> s.send(new ShutdownEvent()));

		HsqlStarter.shutdown((Server) ctx.getAttribute(HsqlStarter.CONTEXT));

		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			try {
				DriverManager.deregisterDriver(drivers.nextElement());
			} catch (SQLException e) {
				LOGGER.error("error while deregistering  driver", e);
			}
		}
		if (!shutdownCleanUpThread("com.mysql.cj.jdbc")) {
			shutdownCleanUpThread("com.mysql.jdbc");
		}

		Messaging.shutdown(env);
		HazelcastConfigurer.shutdown();
		shutDownExecutor(messagingExecutor);
		LOGGER.info("appNG stopped");
		LOGGER.info(StringUtils.leftPad("", 100, "="));
	}

	public boolean shutdownCleanUpThread(String packagePrefix) {
		try {
			Class<?> abandonedConnectionCleanupThread = getClass().getClassLoader()
					.loadClass(packagePrefix + ".AbandonedConnectionCleanupThread");
			Method checkedShutdown = abandonedConnectionCleanupThread.getDeclaredMethod("checkedShutdown");
			checkedShutdown.invoke(null);
			LOGGER.info("Called {}.checkedShutdown()", abandonedConnectionCleanupThread.getName());
			Thread.sleep(5000);
			return true;
		} catch (ClassNotFoundException e) {
			LOGGER.warn("AbandonedConnectionCleanupThread not present");
		} catch (Exception e) {
			LOGGER.warn("error while calling AbandonedConnectionCleanupThread.shutdown()", e);
		}
		return false;
	}

	public void shutDownExecutor(ExecutorService executor) {
		if (null != executor) {
			executor.shutdownNow();
		}
	}

	protected InitializerService getService(Environment env) {
		ApplicationContext platformCtx = env.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
		return platformCtx.getBean(InitializerService.class);
	}

}
