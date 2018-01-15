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
package org.appng.cli.commands.template;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.service.CoreService;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Deletes a template.<br/>
 * 
 * <pre>
 * Usage: appng delete-template [options]
 *   Options:
 *   * -n
 *        The name of the template.
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Parameters(commandDescription = "Deletes a template.")
public class DeleteTemplate implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The name of the template.")
	private String templateName;

	public void execute(CliEnvironment cle) throws BusinessException {
		CoreService coreService = cle.getCoreService();
		Integer state = coreService.deleteTemplate(templateName);
		switch (state) {
		case 0:
			cle.setResult(templateName + " deleted.");
			break;
		case -1:
			cle.setResult("no such template: " + templateName);
			break;
		case -2:
			cle.setResult(templateName + " is in use and can not be deleted.");
			break;
		}
	}
}
