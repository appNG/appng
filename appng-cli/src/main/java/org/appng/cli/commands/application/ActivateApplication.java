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
package org.appng.cli.commands.application;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Links a application to a site.<br/>
 * 
 * <pre>
 * Usage: appng activate-application [options]
 *   Options:
 *   * -a
 *        The application name.
 *   * -s
 *        The site name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Links a application to a site.")
public class ActivateApplication extends BaseApplication implements ExecutableCliCommand {

	@Parameter(names = "-s", required = true, description = "The site name.")
	private String siteName;

	@Parameter(names = "-a", required = true, description = "The application name.")
	private String applicationName;

	public void execute(CliEnvironment cle) throws BusinessException {
		execute(cle, siteName, applicationName, Mode.LINK);
	}

}
