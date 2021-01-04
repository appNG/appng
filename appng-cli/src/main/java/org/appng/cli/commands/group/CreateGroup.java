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

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.GroupImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Creates a group.<br/>
 * 
 * <pre>
 * Usage: appng create-group [options]
 *   Options:
 *     -d
 *        The group description.
 *        Default: <empty string>
 *   * -n
 *        The group name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Creates a group.")
public class CreateGroup implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The group name.")
	private String name;

	@Parameter(names = "-d", required = false, description = "The group description.")
	private String description = "";

	public CreateGroup() {

	}

	CreateGroup(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		GroupImpl group = new GroupImpl();
		group.setName(name);
		group.setDescription(description);
		cle.getCoreService().createGroup(group);
	}

}
