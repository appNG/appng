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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
import org.appng.core.service.HsqlStarter;
import org.appng.core.service.InitializerService;
import org.appng.core.service.MigrationService;
import org.hsqldb.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StopWatch;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * This {@link ServletContextListener} is used to initialize the appNG platform. This includes loading the configuration
 * from {@value #CONFIG_LOCATION}, initializing the {@link ApplicationContext} from
 * {@code /WEB-INF/conf/platformContext.xml} and loading all active {@link Site}s with their configured
 * {@link Application}s.
 * 
 * @author Matthias Müller
 */
public class PlatformStartup implements ServletContextListener {

	private static final Logger log = LoggerFactory.getLogger(PlatformStartup.class);

	public static final String CONFIG_LOCATION = "/WEB-INF/conf/appNG.properties";
	private static final String CONTEXT_LOCATION = "/WEB-INF/conf/platformContext.xml";
	private ExecutorService executor;

	public void contextInitialized(ServletContextEvent sce) {
		try {
			StopWatch startupWatch = new StopWatch("startup");
			startupWatch.start();
			log.info("");
			log.info("Launching appNG, the Next Generation Application Platform ...");
			log.info("");
			log.info("  -  - ---- ----------------------------------------- -------- -  -  ");
			log.info("              ____      _______         _____       .---,,--,-^²²^-,.");
			log.info("     .,--^^²´´    `^^²´´       `²^-.-²´´     ``²---´ dSb. db ,dS$P² <");
			log.info("  ,-´_.dSS$§§§§$Sb. .dS$§§§§§$Sb. : .dS$§§§§$SSb. :  $§4b.$$ l$´ ss ;");
			log.info(",/ .dS$SP²^^^²4$§§$ $§§$P²^^^²4S$$b.`4$$P²^^^²4S§$b. 4$`4b$P `4S$SP |");
			log.info("! .$§SP°       $§§$ $§§$´      `4S§$.`4$´      `4S§$. .-----^²------´ ");
			log.info("; [$§$l        $§§$ $§§$        l$§$] l$        l$§$] !               ");
			log.info("l.`S$Sb.       $§§$ $§§$       ,dS$S´ d$       ,dS$S´ ;               ");
			log.info(" \\ `4S$Sbsaaasd$§§$ $§§$ssaaasdS$SP´,d$$ssaaasdS$SP´ l                ");
			log.info("  \\_ ``4SS$§§§§§§§$ $§§§§§§§$SSP´´  $§§§§§§§$SSP´´_.^                 ");
			log.info("    `----,_,.____,. $$$$  ___.___,^ $$$$  ___.,--^                    ");
			log.info(" _  _ ______ ____ ! $§§$ ! ____ _ ! $$$$ | ________ _______ ____ _  _ ");
			log.info("                  | $$$$ ;        l $$$$ ;                            ");
			log.info("                  `--,.-^´        `--,.-^´");
			log.info("");
			ServletContext ctx = sce.getServletContext();
			Environment env = DefaultEnvironment.get(ctx);

			Properties config = new Properties();
			InputStream configIs = ctx.getResourceAsStream(CONFIG_LOCATION);
			config.load(configIs);
			configIs.close();

			Server hsqlServer = HsqlStarter.startHsql(config, ctx.getRealPath(""));
			if (null != hsqlServer) {
				ctx.setAttribute(HsqlStarter.CONTEXT, hsqlServer);
			}

			DatabaseConnection platformConnection = new MigrationService().initDatabase(config);
			log.info("Platform connection: {}", platformConnection);

			initPlatformContext(ctx, env, config, platformConnection);
			InitializerService service = getService(env, ctx);
			ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
			ThreadFactory threadFactory = tfb.setDaemon(true).setNameFormat("appng-messaging").build();
			executor = Executors.newSingleThreadExecutor(threadFactory);
			service.initPlatform(config, env, platformConnection, ctx, executor);
			startupWatch.stop();
			String appngVersion = env.getAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION);
			log.info("appNG {} started in {} ms.", appngVersion, startupWatch.getTotalTimeMillis());
			log.info(StringUtils.leftPad("", 100, "="));
		} catch (Exception e) {
			log.error("error during platform startup", e);
			contextDestroyed(sce);
		}
	}

	protected void initPlatformContext(ServletContext ctx, Environment env, Properties config,
			DatabaseConnection platformConnection) throws IOException {
		XmlWebApplicationContext platformCtx = new XmlWebApplicationContext();
		platformCtx.setDisplayName("appNG platform context");
		platformCtx.setConfigLocation(CONTEXT_LOCATION);
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
		ConfigurableApplicationContext platformCtx = env.removeAttribute(Scope.PLATFORM,
				Platform.Environment.CORE_PLATFORM_CONTEXT);
		if (null != platformCtx) {
			getService(env, ctx).shutdownPlatform(ctx);
			org.apache.commons.logging.LogFactory.release(platformCtx.getClassLoader());
			platformCtx.close();
		}

		HsqlStarter.shutdown((Server) ctx.getAttribute(HsqlStarter.CONTEXT));

		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			try {
				DriverManager.deregisterDriver(drivers.nextElement());
			} catch (SQLException e) {
				log.error("error while deregistering  driver", e);
			}
		}
		try {
			Class<?> abandonedConnectionCleanupThread = getClass().getClassLoader()
					.loadClass("com.mysql.jdbc.AbandonedConnectionCleanupThread");
			abandonedConnectionCleanupThread.getDeclaredMethod("shutdown").invoke(null);
			Thread.sleep(5000);
		} catch (ClassNotFoundException e) {
			log.debug("AbandonedConnectionCleanupThread not present");
		} catch (Exception e) {
			log.warn("error while calling AbandonedConnectionCleanupThread.shutdown()", e);
		}
		Messaging.shutdown(env);
		if (null != executor) {
			executor.shutdownNow();
		}
		log.info("appNG stopped.");
		log.info(StringUtils.leftPad("", 100, "="));
	}

	protected InitializerService getService(Environment env, ServletContext ctx) {
		ApplicationContext platformCtx = env.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
		return platformCtx.getBean(InitializerService.class);
	}

}
