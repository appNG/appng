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
package org.appng.cli.commands.group;

import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.cli.prettytable.PrettyTable;
import org.appng.cli.prettytable.TableConstants;
import org.appng.core.domain.GroupImpl;

public class CommandListGroupsTest extends AbstractCommandTest {

	private ListGroups command;

	public ExecutableCliCommand getCommand() {
		GroupImpl group = new GroupImpl();
		group.setName("users");
		group.setDescription("foobar");
		cliEnv.getCoreService().createGroup(group);
		command = new ListGroups();
		return command;
	}

	public void validate() {
		PrettyTable prettyTable = command.getPrettyTable();
		validateTableSize(prettyTable, 1);
		validateCell(prettyTable, 1, TableConstants.ID, "1");
		validateCell(prettyTable, 1, TableConstants.NAME, "users");
	}

}
