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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.appng.api.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.util.WebAppRootListener;

/**
 * A {@link ServletContextListener} to initialize log4j2 logging.
 * 
 * @author Matthias Müller
 */
public class Log4jConfigurer extends WebAppRootListener {

	private static Logger LOGGER;
	protected static final String LOG4J_PROPERTIES = "/conf/log4j2.xml";
	public static final String WEB_INF = "/WEB-INF";

	public void contextInitialized(ServletContextEvent sce) {
		super.contextInitialized(sce);
		ServletContext ctx = sce.getServletContext();
		String log4jLocation = ctx.getRealPath(WEB_INF + LOG4J_PROPERTIES);
		String appngData = System.getProperty(Platform.Property.APPNG_DATA);
		if (!StringUtils.isBlank(appngData)) {
			Path log4jPath = Paths.get(appngData, LOG4J_PROPERTIES);
			if (log4jPath.toFile().exists()) {
				log4jLocation = log4jPath.toUri().toString();
			}
		}
		try {
			LoggerContext loggerCtx = LoggerContext.getContext(false);
			URL url = ResourceUtils.getURL(log4jLocation);
			Configuration config = new XmlConfigurationFactory().getConfiguration(loggerCtx, "appNG", url.toURI());
			loggerCtx.setConfiguration(config);
			loggerCtx.start();
			LOGGER = LoggerFactory.getLogger(Log4jConfigurer.class);
			LOGGER.info("Initialized log4j from {}", log4jLocation);
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
	}

}
