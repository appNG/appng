/*
 * Copyright 2011-2020 the original author or authors.
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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.jar.JarInputStream;

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
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.HazelcastConfigurer;
import org.appng.core.service.HsqlStarter;
import org.appng.core.service.InitializerService;
import org.appng.core.service.MigrationService;
import org.appng.core.service.PlatformProperties;
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

	static final String APPNG_STARTED = "APPNG_STARTED";
	public static final String APPNG_CONTEXT = "appNG platform context";
	public static final String CONFIG_LOCATION = "/conf/appNG.properties";
	public static final String WEB_INF = "/WEB-INF";
	private ExecutorService executor;
	private ExecutorService startUpExecutor;

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

			Environment env = DefaultEnvironment.get(ctx);

			Properties config = new Properties();
			config.load(configIs);
			configIs.close();

			Server hsqlServer = HsqlStarter.startHsql(config, ctx.getRealPath(""));
			if (null != hsqlServer) {
				ctx.setAttribute(HsqlStarter.CONTEXT, hsqlServer);
			}

			DatabaseConnection platformConnection = new MigrationService().initDatabase(config);
			LOGGER.info("Platform connection: {}", platformConnection);

			initPlatformContext(ctx, env, config, platformConnection);
			InitializerService service = getService(env);
			ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
			ThreadFactory threadFactory = tfb.setDaemon(true).setNameFormat("appng-messaging").build();
			executor = Executors.newSingleThreadExecutor(threadFactory);

			final PlatformProperties platformProperties = service.loadPlatformProperties(config, env);

			Runnable startUp = () -> {
				try {
					service.initPlatform(platformProperties, env, platformConnection, ctx, executor);
					startupWatch.stop();
					String appngVersion = env.getAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION);
					LOGGER.info("appNG {} started in {} ms.", appngVersion, startupWatch.getTotalTimeMillis());
					LOGGER.info(StringUtils.leftPad("", 100, "="));
					ctx.setAttribute(APPNG_STARTED, true);
				} catch (Exception e) {
					LOGGER.error("error during platform startup", e);
					contextDestroyed(sce);
				}
			};
			startUpExecutor = Executors
					.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("appNG Startup").build());
			startUpExecutor.execute(startUp);
		} catch (Exception e) {
			LOGGER.error("error during platform startup", e);
			contextDestroyed(sce);
		}
	}

	private void printLogo() throws URISyntaxException, IOException, FileNotFoundException {
		LOGGER.info("");
		LOGGER.info(StringUtils.repeat("-", 48));
		String appNGVersion = "appNG.version";
		String jarPath = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		if (jarPath.startsWith("jar:file")) {
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
		ServletContext ctx = sce.getServletContext();
		DefaultEnvironment env = DefaultEnvironment.get(ctx);
		InitializerService initializerService = getService(env);
		if (null != initializerService) {
			initializerService.shutdownPlatform(ctx);
			ConfigurableApplicationContext platformCtx = env.removeAttribute(Scope.PLATFORM,
					Platform.Environment.CORE_PLATFORM_CONTEXT);
			org.apache.commons.logging.LogFactory.release(platformCtx.getClassLoader());
			platformCtx.close();
		}

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
		shutDownExecutor(executor);
		shutDownExecutor(startUpExecutor);
		LOGGER.info("appNG stopped.");
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
