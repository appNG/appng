/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.cli.commands.property;

import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.SiteProperties;
import org.appng.api.model.Property;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.cli.commands.site.CommandCreateSiteTest;
import org.appng.cli.prettytable.PrettyTable;
import org.appng.cli.prettytable.TableConstants;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.service.PropertySupport;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link ListProperties}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CommandListPropertiesTest extends AbstractCommandTest {

	private static final int NUM_SITE_PROPERTIES = 59;
	private static final int NUM_PLATFORM_PROPERTIES = 49;
	private static final int PROP_ROOT_PATH_IDX = 32;
	private ListProperties commandListProperties = new ListProperties();

	@Override
	public ExecutableCliCommand getCommand() {
		return commandListProperties;
	}

	@Override
	public void validate() {
		PrettyTable prettyTable = commandListProperties.getPrettyTable();
		validateTableSize(prettyTable, NUM_PLATFORM_PROPERTIES);
		validateCell(prettyTable, PROP_ROOT_PATH_IDX, TableConstants.NAME,
				PropertySupport.PREFIX_PLATFORM + Platform.Property.PLATFORM_ROOT_PATH);
		validateCell(prettyTable, NUM_PLATFORM_PROPERTIES, TableConstants.NAME,
				PropertySupport.PREFIX_PLATFORM + Platform.Property.XSS_PROTECT);
	}

	@Test
	public void testSiteProperties() throws BusinessException {
		ExecutableCliCommand command = new CommandCreateSiteTest().getCommand();
		command.execute(cliEnv);
		ListProperties commandListSiteProperties = new ListProperties("appng");
		commandListSiteProperties.execute(cliEnv);
		PrettyTable prettyTable = commandListSiteProperties.getPrettyTable();
		int tableSize = NUM_SITE_PROPERTIES;
		validateTableSize(prettyTable, tableSize);
		validateCell(prettyTable, tableSize, TableConstants.NAME,
				"platform.site.appng." + SiteProperties.XSS_EXCEPTIONS);
	}

	@Test
	public void testValues() {
		new RowChecker("name", "value", "value").check(true);
		new RowChecker("name", "", "value").check(true);
		new RowChecker("name", null, "value").check(true);
	}

	@Test
	public void testFilter() {
		new RowChecker("name", null, "value").setFilter("auth").check(false);
		new RowChecker("myAuthentication", "", "value").setFilter("auth").check(true);
		new RowChecker("applicationName", "appng-authentication", "value").setFilter("auth").check(true);
		new RowChecker("applicationName", "", "appng-authentication").setFilter("auth").check(true);
	}

	@Test
	public void testChangedValues() {
		new RowChecker("name", "", "value").showOnlyChangedValues().check(false);
		new RowChecker("name", "changed value", "value").showOnlyChangedValues().check(true);
	}

	@Test
	public void testChangedValuesFilter() {
		new RowChecker("applicationName", "appng-authentication", "value").setFilter("auth").showOnlyChangedValues()
				.check(true);
		new RowChecker("applicationName", "", "appng-authentication").setFilter("auth").showOnlyChangedValues()
				.check(false);
	}

	private class RowChecker {

		private final String name;
		private final String value;
		private final String defaultValue;
		private boolean showOnlyChangedValues = false;
		private String filter = null;

		private RowChecker(String name, String value, String defaultValue) {
			this.name = name;
			this.value = value;
			this.defaultValue = defaultValue;
		}

		private RowChecker showOnlyChangedValues() {
			showOnlyChangedValues = true;
			return this;
		}

		private RowChecker setFilter(String filter) {
			this.filter = filter;
			return this;
		}

		private void check(boolean expected) {
			Property property = new PropertyImpl(name, value, defaultValue);
			Assert.assertEquals(expected, commandListProperties.printRow(property, showOnlyChangedValues, filter));
		}

	}

}
