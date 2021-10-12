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
package org.appng.cli.commands.subject;

import org.appng.api.BusinessException;
import org.appng.api.model.Subject;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Deletes a subject.<br/>
 * 
 * <pre>
 * Usage: appng delete-subject [options]
 *   Options:
 *   * -n
 *        The subject name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 */
@Parameters(commandDescription = "Deletes a subject.")
public class DeleteSubject implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The subject name.")
	private String subjectName;

	public DeleteSubject() {
	}

	public DeleteSubject(String subjectName) {
		this.subjectName = subjectName;
	}

	public void execute(CliEnvironment cle) throws BusinessException {

		Subject subject = cle.getCoreService().getSubjectByName(subjectName, false);

		if (null != subject) {
			cle.getCoreService().deleteSubject(subject);
		} else {
			throw new BusinessException("Subject not found: " + subjectName);
		}

	}

}
