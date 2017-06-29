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
package org.appng.cli.commands.template;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.NoSuchRepositoryException;
import org.appng.core.model.Repository;
import org.appng.core.service.CoreService;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Installs a template.<br/>
 * 
 * <pre>
 * Usage: appng install-template [options]
 *   Options:
 *   * -n
 *        The name of the template.
 *   * -r
 *        The name of the repository.
 *     -t
 *        The timestamp of the template.
 *   * -v
 *        The version of the template.
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Parameters(commandDescription = "Installs a template.")
public class InstallTemplate implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The name of the template.")
	private String templateName;

	@Parameter(names = "-v", required = true, description = "The version of the template.")
	private String templateVersion;

	@Parameter(names = "-t", required = false, description = "The timestamp of the template.")
	private String templateTimestamp;

	@Parameter(names = "-r", required = true, description = "The name of the repository.")
	private String repositoryName;

	public void execute(CliEnvironment cle) throws BusinessException {
		Repository repository = cle.getCoreService().getApplicationRepositoryByName(repositoryName);
		if (null == repository) {
			throw new NoSuchRepositoryException(repositoryName);
		}
		CoreService coreService = cle.getCoreService();
		coreService.provideTemplate(repository.getId(), templateName, templateVersion, templateTimestamp);
	}
}
