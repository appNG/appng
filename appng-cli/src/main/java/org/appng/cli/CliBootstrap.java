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
package org.appng.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.appng.api.Platform;
import org.appng.core.controller.PlatformStartup;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.HsqlStarter;
import org.hsqldb.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StopWatch;

/**
 * Main entry-point to the appNG command line interface, responsible for bootstrapping. Creates an
 * {@link ApplicationContext} and delegates to {@link CliCore}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CliBootstrap {

	public static final String APPNG_HOME = "APPNG_HOME";
	private static final Logger LOG = LoggerFactory.getLogger(CliBootstrap.class);
	static final String CLI_CONTEXT_XML = "cliContext.xml";

	/**
	 * Runs the command line interface and calls {@code System#exit(int)} with the status provided by
	 * {@link #run(String[])}.
	 * 
	 * @param args
	 *            the command line arguments
	 * @throws IOException
	 *             if {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION} could not be found
	 * @see #run(String[])
	 */
	public static void main(String[] args) throws IOException {
		int status = run(args);
		System.exit(status);
	}

	/**
	 * Runs the command line interface
	 * 
	 * @param args
	 *            the command line arguments
	 * @return the execution status:
	 *         <ul>
	 *         <li>{@value org.appng.cli.CliCore#STATUS_OK} - if everything went well</li>
	 *         <li>{@value org.appng.cli.CliCore#DATABASE_ERROR} - if the database is in an erroneous state</li>
	 *         <li>{@value org.appng.cli.CliCore#COMMAND_EXECUTION_ERROR} - if an error occurred while executing the
	 *         command</li>
	 *         <li>{@value org.appng.cli.CliCore#COMMAND_INVALID} - if there is no such command</li>
	 *         <li>{@value org.appng.cli.CliCore#OPTION_INVALID} - if an invalid option was added to the command</li>
	 *         <li>{@value org.appng.cli.CliCore#OPTION_MISSING} - if the command is missing some options</li>
	 *         </ul>
	 * @throws IOException
	 *             if {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION} could not be found
	 */
	public static int run(String[] args) throws IOException {
		StopWatch cliWatch = new StopWatch("cli");
		cliWatch.start();

		CliCore cliCore = new CliCore();
		if (cliCore.processCommand(args)) {

			CliBootstrapEnvironment env = new CliBootstrapEnvironment();

			File platformRootPath = null;
			String appngHome = System.getProperty(APPNG_HOME);
			if (null != appngHome) {
				platformRootPath = new File(appngHome).getAbsoluteFile();
			}
			if (null == platformRootPath || !platformRootPath.exists()) {
				platformRootPath = getPlatformRootPath(env);
				LOG.info("{}: {}", APPNG_HOME, platformRootPath);
			}
			Properties cliConfig = getCliConfig(env, true, platformRootPath);

			Server hsqlServer = HsqlStarter.startHsql(cliConfig, platformRootPath.getAbsolutePath());

			try {
				ConfigurableApplicationContext context = getContext(cliConfig, CLI_CONTEXT_XML);
				cliCore.setContext(context);
				cliCore.perform(cliConfig);

				cliWatch.stop();
				LOG.info("duration: {}ms", cliWatch.getTotalTimeMillis());
				context.close();
				HsqlStarter.shutdown(hsqlServer);
			} catch (BeansException e) {
				cliCore.logError("error while building context, see logs for details.");
				LOG.error("error while building context", e);
				return CliCore.COMMAND_EXECUTION_ERROR;
			}
		}
		return cliCore.getStatus();
	}

	static ConfigurableApplicationContext getContext(final Properties config, String location)
			throws BeansException {
		PropertyResourceConfigurer appNGConfigurer = new PropertySourcesPlaceholderConfigurer();
		appNGConfigurer.setProperties(config);
		ConfigurableApplicationContext platformContext = new ClassPathXmlApplicationContext(new String[] { location },
				false);
		platformContext.addBeanFactoryPostProcessor(appNGConfigurer);
		platformContext.refresh();
		return platformContext;
	}

	private static File checkFile(String name, File file, boolean isDirectory) {
		if (null == file) {
			throw new IllegalArgumentException(name + " is not defined!");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("The path specified in " + name + " does not exist: "
					+ file.getAbsolutePath());
		}
		if (isDirectory && !file.isDirectory()) {
			throw new IllegalArgumentException("The path specified in " + name + " must point to a directory: "
					+ file.getAbsolutePath());
		} else if (!isDirectory && !file.isFile()) {
			throw new IllegalArgumentException("The path specified in " + name + " must point to a file: "
					+ file.getAbsolutePath());
		}
		return file;
	}

	static Properties getCliConfig(CliBootstrapEnvironment env, boolean logInfo, File platformRootPath) throws FileNotFoundException,
			IOException {
		Properties config = new Properties();
		File properties = env.getAbsoluteFile(new File(platformRootPath, PlatformStartup.CONFIG_LOCATION));
		if (properties.exists()) {
			if (logInfo) {
				LOG.info("Using configuration file: " + properties.getAbsolutePath());
			}
			config.load(new FileReader(properties));
		} else {
			throw new FileNotFoundException("Configuration file not found: " + properties.getAbsolutePath());
		}
		config.setProperty(Platform.Property.PLATFORM_ROOT_PATH, platformRootPath.getAbsolutePath());
		config.put(DatabaseService.DATABASE_TYPE, config.getProperty(DatabaseService.DATABASE_TYPE).toUpperCase());
		return config;
	}

	static File getPlatformRootPath(CliBootstrapEnvironment env) {
		// make sure APPNG_HOME is set and valid
		File appngHome = env.getFileFromEnv(APPNG_HOME);
		return checkFile(APPNG_HOME, appngHome, true);
	}

}
