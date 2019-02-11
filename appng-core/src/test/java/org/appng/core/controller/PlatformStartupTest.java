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
package org.appng.core.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.service.InitializerService;
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
	public void testPlatformStartup() throws InvalidConfigurationException, SQLException {
		MockitoAnnotations.initMocks(this);
		Mockito.when(servContext.getRealPath("WEB-INF/lib")).thenReturn("");
		ConcurrentMap<String, Object> platformEnv = new ConcurrentHashMap<>();
		Mockito.when(servContext.getAttribute(Mockito.eq(Scope.PLATFORM.name()))).thenReturn(platformEnv);
		InputStream configResource = getClass().getClassLoader().getResourceAsStream(WEB_INF.substring(1) + CONFIG_LOCATION);
		Mockito.when(servContext.getResourceAsStream(WEB_INF + CONFIG_LOCATION)).thenReturn(configResource);
		URL log4jResource = getClass().getClassLoader().getResource(LOG4J_PROPERTIES.substring(6));
		Mockito.when(servContext.getRealPath(WEB_INF + LOG4J_PROPERTIES)).thenReturn(log4jResource.getPath());
		Mockito.when(servContext.getRealPath("")).thenReturn("target");
		contextInitialized(new ServletContextEvent(servContext));
		Assert.assertTrue(platformEnv.get(Platform.Environment.CORE_PLATFORM_CONTEXT).equals(platformCtx));
		Mockito.verify(initializerService).initPlatform(Mockito.isA(Properties.class), Mockito.isA(Environment.class),
				Mockito.isA(DatabaseConnection.class), Mockito.eq(servContext), Mockito.isA(ExecutorService.class));
		contextDestroyed(new ServletContextEvent(servContext));
		Mockito.verify(initializerService).shutdownPlatform(servContext);
		Mockito.verify(platformCtx).close();
		Assert.assertTrue(platformEnv.isEmpty());
		DriverManager.registerDriver(new org.hsqldb.jdbc.JDBCDriver());
	}

	@Override
	protected InitializerService getService(Environment env, ServletContext ctx) {
		return initializerService;
	}

	@Override
	protected void initPlatformContext(ServletContext ctx, Environment env, Properties properties,
			DatabaseConnection platformConnection) throws IOException {
		env.setAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT, platformCtx);
	}

}
