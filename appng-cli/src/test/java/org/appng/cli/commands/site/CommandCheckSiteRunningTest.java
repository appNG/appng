/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.cli.commands.site;

import org.appng.api.BusinessException;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CommandCheckSiteRunningTest extends AbstractCommandTest {

	CheckSiteRunning command;

	private final String expectedVersion = "1.0.0-M3";

	public ExecutableCliCommand getCommand() {
		try {
			new CreateSite("appng", "localhost", "localhost:8080", "a site", true, true).execute(cliEnv);
			command = new CheckSiteRunning("appng") {
				@Override
				String getContent(String uri) {
					this.responseCode = 200;
					return XML_PREFIX + "<platform version=\"" + expectedVersion + "\" />";
				}
			};
			return command;
		} catch (BusinessException e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}

	public void validate() {
		Assert.assertEquals(expectedVersion, command.getVersion());
		Assert.assertEquals(200, command.getResponseCode());
	}

	@Test
	public void testInvalidUri() throws BusinessException {
		new CreateSite("test", "test", "localhorst:8080", "a site", true, true).execute(cliEnv);
		command = new CheckSiteRunning("test");
		command.execute(cliEnv);
		Assert.assertNull(command.getVersion());
		Assert.assertEquals(-1, command.getResponseCode());
	}

	@Test
	public void testInvalidResponse() throws BusinessException {
		new CreateSite("test2", "test2", "example.com", "a site", true, true).execute(cliEnv);
		command = new CheckSiteRunning("test2");
		command.execute(cliEnv);
		Assert.assertNull(command.getVersion());
		Assert.assertEquals(404, command.getResponseCode());
	}

}
