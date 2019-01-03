/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.cli.commands.permission;

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Adds one ore more permissions to a role.<br/>
 * 
 * <pre>
 * Usage: appng add-permission [options]
 *   Options:
 *   * -n
 *        The permission(s) to add, multiple values can be provided (separated by
 *        space).
 *   *  -a
 *        The application name.
 *   * -r
 *        The role name.
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Parameters(commandDescription = "Adds one ore more permissions to a role.")
public class AddPermission implements ExecutableCliCommand {

	@Parameter(names = "-a", required = true, description = "The application name.")
	private String applicationName;

	@Parameter(names = "-r", required = true, description = "The role name.")
	private String roleName;

	@Parameter(names = "-n", required = true, variableArity = true, description = "The permission(s) to add, multiple values can be provided (separated by space).")
	private List<String> permissionNames;

	public AddPermission() {

	}

	protected AddPermission(String applicationName, String roleName, List<String> permissionNames) {
		this.applicationName = applicationName;
		this.roleName = roleName;
		this.permissionNames = permissionNames;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		cle.getCoreService().addPermissions(applicationName, roleName, permissionNames);
	}

}
