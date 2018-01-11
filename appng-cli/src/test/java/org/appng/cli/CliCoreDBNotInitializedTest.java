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
import java.io.PrintStream;
import java.util.Properties;

import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.cli.commands.AbstractCommandTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = CliCoreDBNotInitializedTest.class, inheritInitializers = false, inheritLocations = true)
public class CliCoreDBNotInitializedTest extends AbstractCommandTest implements
		ApplicationContextInitializer<ConfigurableApplicationContext> {

	private CliCore cliCore = new CliCore();
	private Properties cliConfig;

	public void initialize(ConfigurableApplicationContext applicationContext) {
		Properties properties = CommandTestInitializer.getProperties();
		properties.remove("hibernate.hbm2ddl.auto");
		properties.put("databaseName", getClass().getSimpleName());
		PropertyResourceConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setProperties(properties);
		applicationContext.addBeanFactoryPostProcessor(configurer);
	}

	@Override
	public void test() throws BusinessException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CliEnvironment.out = new PrintStream(out);
		cliCore.processCommand(new String[] { "-m" });
		int result = cliCore.perform(cliConfig);
		Assert.assertEquals(CliCore.DATABASE_ERROR, result);
		Assert.assertTrue(out.toString().startsWith("ERROR: Database is not initialized."));
	}

	@Test
	public void testNoArgs() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CliEnvironment.out = new PrintStream(out);
		cliCore.processCommand(new String[0]);
		int result = cliCore.getStatus();
		Assert.assertEquals(CliCore.STATUS_OK, result);
		Assert.assertTrue(out.toString().contains("Usage: appng [options] [command] [command options]"));
	}

	@Test
	public void testHelp() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		CliEnvironment.out = new PrintStream(out);
		cliCore.processCommand(new String[] { "-h" });
		int result = cliCore.getStatus();
		Assert.assertEquals(CliCore.STATUS_OK, result);
		Assert.assertTrue(out.toString().contains("Usage: appng [options] [command] [command options]"));
	}

	@Before
	@Override
	public void setup() {
		cliCore = new CliCore();
		cliCore.setContext(context);
		cliConfig = CommandTestInitializer.getProperties();
		cliConfig.setProperty(Platform.Property.PLATFORM_ROOT_PATH, CliBootstrapTest.TARGET
				+ CliBootstrapTest.BOOTSTRAP_ROOT);
	}

	@Override
	public ExecutableCliCommand getCommand() {
		return null;
	}

	@Override
	public void validate() {

	}

}
