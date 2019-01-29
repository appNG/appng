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
package org.appng.cli.commands.application;

import java.util.Collection;

import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.NoSuchSiteException;
import org.appng.cli.commands.CommandList;
import org.appng.cli.prettytable.TableConstants;
import org.appng.core.domain.SiteImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Lists the available applications/the applications installed for a site.<br/>
 * 
 * <pre>
 * Usage: appng list-applications [options]
 *   Options:
 *     -s
 *       The site name to list the installed applications for.
 *     -t
 *        Prints tab-separated values instead of a table.
 *        Default: false
 *     -v
 *        Verbose output.
 *        Default: false
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Lists the available applications/the applications installed for a site.")
public class ListApplications extends CommandList implements ExecutableCliCommand {

	@Parameter(names = "-s", description = "The site name to list the installed applications for.")
	private String siteName;

	public ListApplications() {

	}

	ListApplications(String siteName) {
		this.siteName = siteName;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		Collection<? extends Application> applications;
		if (null == siteName) {
			applications = cle.getCoreService().getApplications();
		} else {
			SiteImpl siteByName = cle.getCoreService().getSiteByName(siteName);
			if (null == siteByName) {
				throw new NoSuchSiteException(siteName);
			}
			applications = siteByName.getApplications();
		}

		if (null != applications) {

			prettyTable.addColumn(TableConstants.ID);
			prettyTable.addColumn(TableConstants.NAME);
			prettyTable.addColumn(TableConstants.DISPLAY_NAME);
			prettyTable.addColumn(TableConstants.PRIVILEGED);
			prettyTable.addColumn(TableConstants.FILEBASED, true);
			prettyTable.addColumn(TableConstants.HIDDEN, true);

			for (Application application : applications) {
				prettyTable.addRow(application.getId(), application.getName(), application.getDisplayName(),
						application.isPrivileged(), application.isFileBased(), application.isHidden());
			}

			cle.setResult(renderTable());

		}

	}

}
