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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.cli.commands.AbstractCommandTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;

import com.beust.jcommander.Parameters;

@ContextConfiguration(initializers = CliCoreTest.class, inheritInitializers = false, inheritLocations = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CliCoreTest extends AbstractCommandTest
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private CliCore cliCore = new CliCore();
	private Properties cliConfig;
	private String command;
	private String message;
	private int status;

	public void initialize(ConfigurableApplicationContext applicationContext) {
		Properties properties = CommandTestInitializer.getProperties(getClass());
		properties.remove("hibernate.hbm2ddl.auto");
		PropertyResourceConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setProperties(properties);
		applicationContext.addBeanFactoryPostProcessor(configurer);
	}

	@Override
	public void test() throws BusinessException {
		// nothing to do
	}

	@Test
	public void testBatch() throws BusinessException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CliEnvironment.out = new PrintStream(out);
		if (cliCore.processCommand(new String[] { "batch", "-f", "src/test/resources/cli-batch.list" })) {
			cliCore.perform(cliConfig);
		}
		int result = cliCore.getStatus();
		Assert.assertEquals(CliCore.STATUS_OK, result);
		Assert.assertTrue(out.toString().contains("-i"));
		Assert.assertTrue(out.toString().contains("list-sites -t"));
		Assert.assertTrue(out.toString().contains("ID	Name	Host	Domain"));
	}

	@Before
	@Override
	public void setup() {
		cliCore = new CliCore() {
			@Override
			void addCommands() {
				commands.add("force-error", new ErrorCommand());
				super.addCommands();
			}
		};
		cliCore.setContext(context);
		cliConfig = CommandTestInitializer.getProperties(getClass());
		cliConfig.setProperty(Platform.Property.PLATFORM_ROOT_PATH,
				CliBootstrapTest.TARGET + CliBootstrapTest.BOOTSTRAP_ROOT);
	}

	@Test
	public void testCommand() {
		init();
		this.message = "ID	Name	Host	Domain";
		this.status = CliCore.OPTION_INVALID;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CliEnvironment.out = new PrintStream(out);
		if (cliCore.processCommand(new String[] { "list-sites", "-t" })) {
			cliCore.perform(cliConfig);
		}
		int result = cliCore.getStatus();
		Assert.assertEquals(CliCore.STATUS_OK, result);
		Assert.assertTrue(out.toString().contains(message));
	}

	@Test
	public void testError() {
		init();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CliEnvironment.out = new PrintStream(out);
		if (cliCore.processCommand(new String[] { "force-error" })) {
			cliCore.perform(cliConfig);
		}
		int result = cliCore.getStatus();
		Assert.assertEquals(CliCore.COMMAND_EXECUTION_ERROR, result);
		Assert.assertTrue(out.toString().contains("ERROR: force error"));
	}

	private void init() {
		this.command = "-i";
		this.status = CliCore.STATUS_OK;
		this.validate();
	}

	@Test
	public void testMissingOptions() throws Exception {
		this.command = "add-permission";
		File file = new File(getClass().getClassLoader().getResource("CliCoreTest-testMissingOptions.txt").toURI());
		this.message = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		this.status = CliCore.OPTION_MISSING;
		validate();
	}

	@Test
	public void testInvalidOptions() {
		this.command = "add-permission -n aaa";
		this.message = "ERROR: Invalid options: -n aaa";
		this.status = CliCore.OPTION_INVALID;
		validate();
	}

	@Test
	public void testInvalidCommand() {
		this.command = "addd-permission";
		this.message = "ERROR: Invalid command: addd-permission";
		this.status = CliCore.COMMAND_INVALID;
		validate();
	}

	@Override
	public void validate() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CliEnvironment.out = new PrintStream(out);
		if (cliCore.processCommand(new String[] { command })) {
			cliCore.perform(cliConfig);
		}
		int result = cliCore.getStatus();
		if (null != message) {
			Assert.assertTrue("expected: " + message + "\nactual:" + out.toString(), out.toString().contains(message));
		}
		Assert.assertEquals(status, result);
	}

	@Override
	public ExecutableCliCommand getCommand() {
		return null;
	}

	@Parameters(commandDescription = "throws BusinessException")
	class ErrorCommand implements ExecutableCliCommand {
		public void execute(CliEnvironment cle) throws BusinessException {
			throw new BusinessException("force error");
		}
	}

}
