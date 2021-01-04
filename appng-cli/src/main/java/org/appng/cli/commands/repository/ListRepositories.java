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
package org.appng.cli.commands.repository;

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.CommandList;
import org.appng.cli.prettytable.TableConstants;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.model.Repository;

import com.beust.jcommander.Parameters;

/**
 * Lists the available repositories.<br/>
 * 
 * <pre>
 * Usage: appng list-repositories [options]
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
@Parameters(commandDescription = "Lists the available repositories.")
public class ListRepositories extends CommandList implements ExecutableCliCommand {

	public void execute(CliEnvironment cle) throws BusinessException {

		List<RepositoryImpl> repositories = cle.getCoreService().getApplicationRepositories();

		if (null != repositories) {

			prettyTable.addColumn(TableConstants.ID);
			prettyTable.addColumn(TableConstants.NAME);
			prettyTable.addColumn(TableConstants.TYPE);
			prettyTable.addColumn(TableConstants.MODE, true);
			prettyTable.addColumn(TableConstants.URI);
			prettyTable.addColumn(TableConstants.IS_ACTIVE, true);
			prettyTable.addColumn(TableConstants.DESCRIPTION, true);

			for (Repository repository : repositories) {
				prettyTable.addRow(repository.getId(), repository.getName(), repository.getRepositoryType(),
						repository.getRepositoryMode(), repository.getUri(), repository.isActive(),
						repository.getDescription());
			}

			cle.setResult(renderTable());

		}

	}
}
