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
package org.appng.cli.commands.site;

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.api.model.Site;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.CommandList;
import org.appng.cli.prettytable.TableConstants;
import org.appng.core.domain.SiteImpl;

import com.beust.jcommander.Parameters;

/**
 * Lists the available sites.
 * 
 * <pre>
 * Usage: appng list-sites [options]
 *   Options:
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
@Parameters(commandDescription = "Lists the available sites.")
public class ListSites extends CommandList implements ExecutableCliCommand {

	public void execute(CliEnvironment cle) throws BusinessException {

		List<SiteImpl> sites = cle.getCoreService().getSites();

		if (null != sites) {

			prettyTable.addColumn(TableConstants.ID);
			prettyTable.addColumn(TableConstants.NAME);
			prettyTable.addColumn(TableConstants.HOST);
			prettyTable.addColumn(TableConstants.DOMAIN);
			prettyTable.addColumn(TableConstants.IS_ACTIVE, true);

			for (Site site : sites) {
				prettyTable.addRow(site.getId(), site.getName(), site.getHost(), site.getDomain(), site.isActive());
			}

			cle.setResult(renderTable());

		}

	}

}
