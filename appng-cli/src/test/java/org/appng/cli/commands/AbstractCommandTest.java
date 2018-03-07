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
package org.appng.cli.commands;

import java.util.Properties;

import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.cli.CliBootstrapTest;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest.CommandTestInitializer;
import org.appng.cli.prettytable.PrettyTable;
import org.appng.cli.prettytable.TableRow;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.beust.jcommander.JCommander;

@Transactional(transactionManager = "coreTxManager")
@Rollback(false)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:cliContext-test.xml" }, initializers = CommandTestInitializer.class)
@DirtiesContext
public abstract class AbstractCommandTest {

	protected CliEnvironment cliEnv;

	@Autowired
	protected ConfigurableApplicationContext context;

	public static class CommandTestInitializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		public CommandTestInitializer() {

		}

		public void initialize(ConfigurableApplicationContext platformContext) {
			Properties config = getProperties(getClass());
			PropertyResourceConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
			configurer.setProperties(config);
			platformContext.addBeanFactoryPostProcessor(configurer);
		}

		public static Properties getProperties(Class<?> caller) {
			Properties config = new Properties();
			config.put("hibernate.connection.url", "jdbc:hsqldb:mem://" + caller.getSimpleName());
			config.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
			config.put("hibernate.connection.driver_class", "org.hsqldb.jdbc.JDBCDriver");
			config.put("hibernate.connection.username", "sa");
			config.put("hibernate.connection.password", "");
			config.put("hibernate.hbm2ddl.auto", "create");
			config.put("hibernate.show_sql", "false");
			config.put("database.type", "HSQL");
			config.put("database.validationPeriod", "15");
			return config;
		}
	}

	@Before
	public void setup() {
		Properties cliConfig = new Properties();
		cliConfig.setProperty(Platform.Property.PLATFORM_ROOT_PATH,
				CliBootstrapTest.TARGET + CliBootstrapTest.BOOTSTRAP_ROOT);
		this.cliEnv = new CliEnvironment(context, cliConfig);
		cliEnv.initPlatform(cliConfig);
	}

	@Test
	public void test() throws BusinessException {
		getCommand().execute(cliEnv);
		validate();
	}

	protected ExecutableCliCommand parse(ExecutableCliCommand command, String... args) {
		new JCommander(command).parse(args);
		return command;
	}

	public abstract ExecutableCliCommand getCommand();

	public abstract void validate() throws BusinessException;

	protected void validateTableSize(PrettyTable prettyTable, int rows) {
		Assert.assertEquals(rows, prettyTable.getRows().size());
	}

	protected void validateCell(PrettyTable prettyTable, int row, String column, String value) {
		TableRow tableRow = prettyTable.getRows().get(row - 1);
		Assert.assertEquals(value, tableRow.getValueAt(prettyTable.getColumnIndex(column)));
	}

}
