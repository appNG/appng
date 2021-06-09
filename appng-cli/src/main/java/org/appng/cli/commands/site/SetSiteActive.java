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
package org.appng.cli.commands.site;

import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Activates/deactivates a site.<br/>
 * 
 * <pre>
 * Usage: appng site-setactive [options]
 *   Options:
 *   * -n
 *        The site name.
 *   * -d
 *        Deactivate the site.
 * </pre>
 * 
 * @author Matthias Müller
 */
@Parameters(commandDescription = "Activates/deactivates a site.")
public class SetSiteActive implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The site name.")
	private String siteName;

	@Parameter(names = "-d", description = "Deactivate the site.")
	private boolean deactivate = false;

	public SetSiteActive() {

	}

	SetSiteActive(String siteName, boolean activate) {
		this.siteName = siteName;
		this.deactivate = !activate;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		cle.getCoreService().setSiteActive(siteName, !deactivate);
	}

}
