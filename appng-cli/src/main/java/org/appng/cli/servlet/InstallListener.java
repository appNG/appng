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
package org.appng.cli.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.appng.cli.CliBootstrap;
import org.appng.cli.CliCore;
import org.appng.cli.CliEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallListener implements ServletContextListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstallListener.class);
	private static final String DATE_PATTERN = "yyyy-MM-dd-HH-mm";
	private static final String BATCH_FILE = "auto-install.list";
	private static final String INSTALL_PATH = "/WEB-INF/conf/" + BATCH_FILE;

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext ctx = sce.getServletContext();
		File autoInstall = null;
		String resource = ctx.getRealPath(INSTALL_PATH);
		if (null == resource) {
			LOGGER.info("{} not present", INSTALL_PATH);
			return;
		}
		String message = null;
		try {
			autoInstall = new File(resource);
			if (autoInstall.exists()) {
				LOGGER.info("processing " + resource);
				System.getProperties().put(CliBootstrap.APPNG_HOME, ctx.getRealPath("/"));
				ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
				CliEnvironment.out = new PrintStream(bytesOut);
				String[] args = new String[] { "batch", "-f", resource };
				int status = CliBootstrap.run(args);
				message = new String(bytesOut.toByteArray());
				LOGGER.debug(message);
				if (CliCore.STATUS_OK != status) {
					LOGGER.warn("CLI returned status {}", status);
				}
				LOGGER.info("done processing {}", resource);
			} else {
				LOGGER.debug("{} not present", resource);
			}
		} catch (Exception e) {
			LOGGER.error("error while processing " + resource, e);
		} finally {
			if (null != autoInstall && autoInstall.exists()) {
				String timestamp = DateFormatUtils.format(System.currentTimeMillis(), DATE_PATTERN);
				String targetName = BATCH_FILE + "." + timestamp;
				File processingResult = new File(autoInstall.getParent(), targetName);
				try {
					if (null != message) {
						FileUtils.write(processingResult, message, Charset.defaultCharset(), false);
						FileUtils.deleteQuietly(autoInstall);
					} else {
						FileUtils.moveFile(autoInstall, processingResult);
					}
				} catch (IOException e) {
					LOGGER.warn("error while creating " + processingResult.getPath(), e);
				}
			}
		}

	}

	public void contextDestroyed(ServletContextEvent sce) {

	}

}
