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
package org.appng.cli.commands.applicationrole;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.api.model.Role;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.CommandList;
import org.appng.cli.prettytable.TableConstants;
import org.appng.core.domain.RoleImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Lists the available application roles for a application.<br/>
 * 
 * <pre>
 * Usage: appng list-roles [options]
 *   Options:
 *     -a
 *        The application name.
 *     -t
 *        Prints tab-separated values instead of a table.
 *        Default: false
 *     -v
 *        Verbose output.
 *        Default: false
 * </pre>
 * 
 * @author Matthias Herlitzius
 */
@Parameters(commandDescription = "Lists the available application roles for a application.")
public class ListRoles extends CommandList implements ExecutableCliCommand {

	@Parameter(names = "-a", required = false, description = "The application name.")
	private String applicationName;

	public void execute(CliEnvironment cle) throws BusinessException {

		List<RoleImpl> applicationRoles = null;
		if (StringUtils.isNotEmpty(applicationName)) {
			Application application = cle.getCoreService().findApplicationByName(applicationName);
			if (null != application) {
				applicationRoles = cle.getCoreService().getApplicationRolesForApplication(application.getId());
			} else {
				throw new BusinessException("no such application: " + applicationName);
			}
		} else {
			applicationRoles = cle.getCoreService().getApplicationRoles();
		}

		if (null != applicationRoles) {

			prettyTable.addColumn(TableConstants.ID);
			prettyTable.addColumn(TableConstants.APPLICATION);
			prettyTable.addColumn(TableConstants.NAME);
			prettyTable.addColumn(TableConstants.DESCRIPTION, true);

			for (Role applicationRole : applicationRoles) {
				prettyTable.addRow(applicationRole.getId(), applicationRole.getApplication().getName(),
						applicationRole.getName(), applicationRole.getDescription());
			}

			cle.setResult(renderTable());

		}

	}
}
