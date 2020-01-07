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
package org.appng.cli.commands.application;

import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.domain.SiteImpl;
import org.junit.Assert;

public class CommandListApplicationsForSiteTest extends AbstractCommandTest {

	public ExecutableCliCommand getCommand() {
		SiteImpl site = new SiteImpl();
		site.setName("testsite");
		site.setHost("example.com");
		site.setDomain("example.com:8080");
		cliEnv.getCoreService().createSite(site);
		return new ListApplications("testsite");
	}

	public void validate() {
		String result = cliEnv.getResult();
		Assert.assertEquals("| ID | Name | Display name | Privileged |",
				result.trim().replaceAll("=", "").replaceAll("\n", ""));
	}
}
