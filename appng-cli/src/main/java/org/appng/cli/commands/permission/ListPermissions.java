/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.api.model.Nameable;
import org.appng.api.model.Permission;
import org.appng.api.model.Role;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.CommandList;
import org.appng.cli.prettytable.TableConstants;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Lists the available {@link Permission}s for a {@link Application}/{@link Role}.<br/>
 * 
 * <pre>
 * Usage: appng list-permissions [options]
 *   Options:
 *   * -a
 *        The application name.
 *     -r
 *        The role name.
 *     -t
 *        Prints tab-separated values instead of a table.
 *        Default: false
 *     -v
 *        Verbose output.
 *        Default: false
 * </pre>
 * 
 * @author Matthias Müller
 */
@Parameters(commandDescription = "Lists the available permissions for a application/a role of a application.")
public class ListPermissions extends CommandList implements ExecutableCliCommand {

	@Parameter(names = "-a", required = true, description = "The application name.")
	private String applicationName;

	@Parameter(names = "-r", required = false, description = "The role name.")
	private String roleName;

	public ListPermissions() {

	}

	protected ListPermissions(String applicationName, String roleName) {
		this.applicationName = applicationName;
		this.roleName = roleName;
	}

	public void execute(CliEnvironment cle) throws BusinessException {

		List<? extends Permission> permissions = null;
		if (StringUtils.isNotEmpty(applicationName)) {
			Application application = cle.getCoreService().findApplicationByName(applicationName);
			if (null != application) {
				if (StringUtils.isNotEmpty(roleName)) {
					Role role = cle.getCoreService().getApplicationRoleForApplication(application.getId(), roleName);
					if (null == role) {
						throw new BusinessException("no such role: " + roleName);
					}
					permissions = new ArrayList<>(role.getPermissions());
				} else {
					permissions = cle.getCoreService().getPermissionsForApplication(application.getId());
				}
				Collections.sort(permissions, new Comparator<Nameable>() {
					public int compare(Nameable n1, Nameable n2) {
						return n1.getName().compareTo(n2.getName());
					}
				});

				prettyTable.addColumn(TableConstants.ID);
				prettyTable.addColumn(TableConstants.NAME);
				prettyTable.addColumn(TableConstants.DESCRIPTION);

				for (Permission permission : permissions) {
					prettyTable.addRow(permission.getId(), permission.getName(), permission.getDescription());
				}
				cle.setResult(renderTable());
			} else {
				throw new BusinessException("no such application: " + applicationName);
			}
		}
	}
}
