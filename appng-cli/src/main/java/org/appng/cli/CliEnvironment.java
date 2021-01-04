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
package org.appng.cli;

import java.io.PrintStream;

import org.appng.api.Platform;
import org.appng.api.model.Properties;
import org.appng.core.service.CoreService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

/**
 * Holds informations about the environment when executing {@link ExecutableCliCommand}s.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CliEnvironment {

	private Boolean devMode = true;
	private String result = null;
	private ApplicationContext platformContext;
	private Properties platformConfig;
	private java.util.Properties cliConfig;
	public static PrintStream out = System.out;

	/**
	 * Creates a new {@link CliEnvironment}.
	 * 
	 * @param platformContext
	 *            the {@link ApplicationContext} used
	 * @param cliConfig
	 *            the configuration read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 */
	public CliEnvironment(ApplicationContext platformContext, final java.util.Properties cliConfig) {
		this.platformContext = platformContext;
		this.cliConfig = cliConfig;
	}

	/**
	 * Initialized the appNG platform.
	 */
	public void initPlatform(java.util.Properties defaultOverrides) {
		String platformRootPath = cliConfig.getProperty(Platform.Property.PLATFORM_ROOT_PATH);
		this.platformConfig = getCoreService().initPlatformConfig(defaultOverrides, platformRootPath, devMode, true, false);
	}

	/**
	 * Returns the appNG platform configuration.
	 * 
	 * @return the appNG platform configuration
	 */
	public Properties getPlatformConfig() {
		return platformConfig;
	}

	/**
	 * Retrieves a {@link CoreService} from the {@link ApplicationContext}.
	 * 
	 * @return the {@link CoreService}
	 */
	public CoreService getCoreService() {
		return platformContext.getBean(CoreService.class);
	}

	/**
	 * Sets the result of a {@link ExecutableCliCommand}-execution, which is a string to output on the console.
	 * 
	 * @param result
	 *            the result to set
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * Retrieves the result of a {@link ExecutableCliCommand}-execution, which is a string to output on the console.
	 */
	public String getResult() {
		return result;
	}

	/**
	 * Retrieves the {@link ApplicationContext}.
	 * 
	 * @return the {@link ApplicationContext}
	 */
	public ApplicationContext getContext() {
		return platformContext;
	}

	/**
	 * Retrieves the cli configuration
	 * 
	 * @return the configuration read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 */
	public java.util.Properties getCliConfig() {
		return cliConfig;
	}

	public MessageSource getMessageSource() {
		return platformContext.getBean(MessageSource.class);
	}

}
