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
package org.appng.cli.commands.applicationrole;

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Adds a application role to a group.<br/>
 * 
 * <pre>
 * Usage: appng add-role [options]
 *   Options:
 *     -c
 *        Clear existing application role of the group.
 *        Default: false
 *   * -g
 *        The group name.
 *   * -a
 *        The application name.
 *   * -r
 *        A application role name. Multiple values can be provided (separated by space).
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Adds a application role to a group.")
public class AddRole implements ExecutableCliCommand {

	@Parameter(names = "-g", required = true, description = "The group name.")
	private String groupName;

	@Parameter(names = "-a", required = true, description = "The application name.")
	private String applicationName;

	@Parameter(names = "-r", required = true, variableArity = true, description = "A application role name. Multiple values can be provided (separated by space).")
	private List<String> applicationRoleNames;

	@Parameter(names = "-c", required = false, description = "Clear existing application role of the group.")
	private boolean clear;

	public void execute(CliEnvironment cle) throws BusinessException {
		try {
			cle.getCoreService().addApplicationRolesToGroup(groupName, applicationName, applicationRoleNames, clear);
		} catch (Exception e) {
			throw new BusinessException("Invalid group or application role id.", e);
		}
	}

}
