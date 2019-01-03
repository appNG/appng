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
package org.appng.appngizer.controller;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.appng.api.messaging.Messaging;
import org.appng.api.support.environment.DefaultEnvironment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppNGizer implements ServletContextListener {

	private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
	static final String APPNG_HOME = "APPNG_HOME";
	static final String APPNG_VERSION = "APPNG_VERSION";

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext servletContext = sce.getServletContext();
		try {
			URL resource = getClass().getClassLoader().getResource("appNGizer.txt");
			List<String> logoLines = Files.readAllLines(new File(resource.toURI()).toPath(), StandardCharsets.UTF_8);
			logoLines.forEach(l -> log.info(l));
			Manifest manifest = new Manifest(servletContext.getResourceAsStream(MANIFEST_MF));
			String version = manifest.getMainAttributes().getValue("Implementation-Version");
			log.info("Starting appNGizer {}", version);
			String appngHome = servletContext.getServletRegistration("appNGizer").getInitParameter(APPNG_HOME);
			log.info("{} = {}", APPNG_HOME, appngHome);
			servletContext.setAttribute(APPNG_VERSION, version);
			servletContext.setAttribute(APPNG_HOME, appngHome);
		} catch (Exception e) {
			if (!(e instanceof IllegalArgumentException)) {
				throw new IllegalStateException(e);
			}
			throw (IllegalArgumentException) e;
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		Messaging.shutdown(DefaultEnvironment.get(sce.getServletContext()));
	}

}
