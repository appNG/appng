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
package org.appng.core.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.service.InitializerService;
import org.appng.core.service.PlatformProperties;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class PlatformStartupTest extends PlatformStartup {

	@Mock
	private ServletContext servContext;

	@Mock
	private ConfigurableWebApplicationContext platformCtx;

	@Mock
	private InitializerService initializerService;

	@Test
	public void testPlatformStartup() throws Exception {
		MockitoAnnotations.initMocks(this);
		AtomicBoolean started = new AtomicBoolean(false);
		Mockito.doAnswer(i -> {
			started.set(true);
			return null;
		}).when(servContext).setAttribute(APPNG_STARTED, true);
		Mockito.when(servContext.getRealPath("/")).thenReturn("");
		Mockito.when(servContext.getRealPath("WEB-INF/lib")).thenReturn("");
		ConcurrentMap<String, Object> platformEnv = new ConcurrentHashMap<>();
		Mockito.when(servContext.getAttribute(Mockito.eq(Scope.PLATFORM.name()))).thenReturn(platformEnv);
		InputStream configResource = getClass().getClassLoader()
				.getResourceAsStream(WEB_INF.substring(1) + CONFIG_LOCATION);
		Mockito.when(servContext.getResourceAsStream(WEB_INF + CONFIG_LOCATION)).thenReturn(configResource);

		URL log4jResource = getClass().getClassLoader().getResource(Log4jConfigurer.LOG4J_PROPERTIES.substring(6));
		Mockito.when(servContext.getRealPath(WEB_INF + Log4jConfigurer.LOG4J_PROPERTIES))
				.thenReturn(log4jResource.getPath());
		Mockito.when(servContext.getRealPath("")).thenReturn("target");
		Mockito.when(servContext.getRealPath(WEB_INF + Log4jConfigurer.LOG4J_PROPERTIES))
				.thenReturn("classpath:log4j.properties");

		PlatformProperties value = PlatformProperties.get(DefaultEnvironment.get(servContext));
		Mockito.when(initializerService.loadPlatformProperties(Mockito.any(), Mockito.any())).thenReturn(value);

		new Log4jConfigurer().contextInitialized(new ServletContextEvent(servContext));

		System.setProperty("appNG", "appNG");
		Properties testProps = new Properties();
		testProps.put("env.path", "${env.PATH}");
		testProps.put("sys.appNG", "${sys.appNG}");
		applySystem(testProps);
		Assert.assertEquals(System.getenv("PATH"), testProps.get("env.path"));
		Assert.assertEquals(System.getProperty("appNG"), testProps.get("sys.appNG"));

		contextInitialized(new ServletContextEvent(servContext));
		Assert.assertTrue(platformEnv.get(Platform.Environment.CORE_PLATFORM_CONTEXT).equals(platformCtx));

		while (!started.get()) {
			TimeUnit.MILLISECONDS.sleep(100);
		}

		Mockito.verify(initializerService).initPlatform(Mockito.isA(PlatformProperties.class),
				Mockito.isA(Environment.class), Mockito.isA(DatabaseConnection.class), Mockito.eq(servContext),
				Mockito.isA(ExecutorService.class), Mockito.isA(ExecutorService.class));
		contextDestroyed(new ServletContextEvent(servContext));
		Mockito.verify(initializerService).shutdownPlatform(servContext);
		Mockito.verify(platformCtx).close();
		Assert.assertTrue(platformEnv.isEmpty());
		DriverManager.registerDriver(new org.hsqldb.jdbc.JDBCDriver());
	}

	@Override
	protected InitializerService getService(Environment env) {
		return initializerService;
	}

	@Override
	protected void initPlatformContext(ServletContext ctx, Environment env, Properties config,
			DatabaseConnection platformConnection) throws IOException {
		env.setAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT, platformCtx);
	}

}
