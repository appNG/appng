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
package org.appng.cli.commands.subject;

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.api.model.Subject;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.CommandList;
import org.appng.cli.prettytable.TableConstants;
import org.appng.core.domain.SubjectImpl;

import com.beust.jcommander.Parameters;

/**
 * Lists all subjects.<br/>
 * 
 * <pre>
 * Usage: appng list-subjects [options]
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
 * 
 */
@Parameters(commandDescription = "Lists all subjects.")
public class ListSubjects extends CommandList implements ExecutableCliCommand {

	public void execute(CliEnvironment cle) throws BusinessException {

		List<SubjectImpl> subjects = cle.getCoreService().getSubjects();

		if (null != subjects) {

			prettyTable.addColumn(TableConstants.ID);
			prettyTable.addColumn(TableConstants.USER_NAME);
			prettyTable.addColumn(TableConstants.REAL_NAME);
			prettyTable.addColumn(TableConstants.EMAIL);
			prettyTable.addColumn(TableConstants.LANGUAGE, true);
			prettyTable.addColumn(TableConstants.LAST_LOGIN);
			prettyTable.addColumn(TableConstants.LOCKED_SINCE);
			prettyTable.addColumn(TableConstants.CHANGE_PASSWORD_ALLOWED);
			prettyTable.addColumn(TableConstants.FAILED_LOGIN_ATTEMPTS);

			for (Subject subject : subjects) {
				prettyTable.addRow(subject.getId(), subject.getName(), subject.getRealname(), subject.getEmail(),
						subject.getLanguage(), subject.getLastLogin(), subject.getLockedSince(),
						subject.isChangePasswordAllowed(), subject.getFailedLoginAttempts());
			}

			cle.setResult(renderTable());

		}

	}

}
