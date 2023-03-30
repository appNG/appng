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
package org.appng.cli.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.appng.api.Platform;
import org.appng.cli.CliBootstrap;
import org.appng.cli.CliCore;
import org.appng.cli.CliEnvironment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstallListener implements ServletContextListener {

	private static final String DATE_PATTERN = "yyyy-MM-dd-HH-mm";
	private static final String BATCH_FILE = "auto-install.list";
	private static final String INSTALL_PATH = "/WEB-INF/conf/" + BATCH_FILE;

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext ctx = sce.getServletContext();
		File autoInstall;
		String appngData = System.getProperty(Platform.Property.APPNG_DATA);
		if (StringUtils.isBlank(appngData)) {
			String resource = ctx.getRealPath(INSTALL_PATH);
			if (null == resource) {
				LOGGER.info("{} not present", INSTALL_PATH);
				return;
			}
			autoInstall = new File(resource);
		} else {
			autoInstall = Paths.get(appngData, "conf", BATCH_FILE).toFile();
		}

		String message = null;
		try {
			if (autoInstall.exists()) {
				LOGGER.info("processing {}", autoInstall);
				System.getProperties().put(CliBootstrap.APPNG_HOME, ctx.getRealPath("/"));
				try (ByteArrayOutputStream bytesOut = new ByteArrayOutputStream()) {
					CliEnvironment.out = new PrintStream(bytesOut);
					String[] args = new String[] { "batch", "-f", autoInstall.getAbsolutePath() };
					int status = CliBootstrap.run(args);
					message = new String(bytesOut.toByteArray());
					LOGGER.debug(message);
					if (CliCore.STATUS_OK != status) {
						LOGGER.warn("CLI returned status {}", status);
					}
					LOGGER.info("done processing {}", autoInstall);
				}
			} else {
				LOGGER.debug("{} not present", autoInstall);
			}
		} catch (Exception e) {
			LOGGER.error(String.format("error while processing %s", autoInstall), e);
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
					LOGGER.warn(String.format("error while creating %s", processingResult.getPath()), e);
				}
			}
		}

	}

	public void contextDestroyed(ServletContextEvent sce) {

	}

}
