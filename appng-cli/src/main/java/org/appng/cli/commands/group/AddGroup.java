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
package org.appng.cli.commands.group;

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Adds a group to a subject.<br/>
 * 
 * <pre>
 *  Usage: appng add-group [options]
 *   Options:
 *     -c
 *        Clear existing groups of the subject.
 *        Default: false
 *   * -g
 *        The group name. Multiple values can be provided (separated by space).
 *   * -u
 *        The user name of the subject.
 * 
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Adds a group to a subject.")
public class AddGroup implements ExecutableCliCommand {

	@Parameter(names = "-u", required = true, description = "The user name of the subject.")
	private String loginName;

	@Parameter(names = "-g", required = true, variableArity = true, description = "The group name. Multiple values can be provided (separated by space).")
	private List<String> groupName;

	@Parameter(names = "-c", required = false, description = "Clear existing groups of the subject.")
	private boolean clear;

	public AddGroup() {

	}

	AddGroup(String loginName, List<String> groupNames) {
		this.loginName = loginName;
		this.groupName = groupNames;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		cle.getCoreService().addGroupsToSubject(loginName, groupName, clear);
	}

}
