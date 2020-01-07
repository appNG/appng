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
package org.appng.cli.commands.site;

import org.appng.api.BusinessException;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Deletes a site.<br/>
 * 
 * <pre>
 * Usage: appng delete-site [options]
 *   Options:
 *   * -n
 *        The site name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Deletes a site.")
public class DeleteSite implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The site name.")
	private String siteName;

	public DeleteSite() {

	}

	DeleteSite(String siteName) {
		this.siteName = siteName;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		CheckSiteRunning checkSiteRunning = new CheckSiteRunning(siteName);
		checkSiteRunning.execute(cle);
		if (checkSiteRunning.isRunning()) {
			throw new BusinessException("The site '" + siteName + "' is currently running and can not be deleted.");
		}
		cle.getCoreService().deleteSite(siteName, new FieldProcessorImpl(null));
	}

}
