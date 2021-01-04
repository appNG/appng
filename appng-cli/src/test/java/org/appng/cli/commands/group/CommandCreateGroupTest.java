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
package org.appng.cli.commands.group;

import org.appng.api.model.Group;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.junit.Assert;

public class CommandCreateGroupTest extends AbstractCommandTest {

	public ExecutableCliCommand getCommand() {
		return new CreateGroup("admins", "appNG Administrators");
	}

	public void validate() {
		Group group = cliEnv.getCoreService().getGroupByName("admins");
		Assert.assertEquals("admins", group.getName());
		Assert.assertEquals("appNG Administrators", group.getDescription());
	}

}
