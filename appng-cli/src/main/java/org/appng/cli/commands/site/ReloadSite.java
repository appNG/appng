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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.appng.api.BusinessException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Reloads a site (site property {@code supportReloadFile} must be {@code true}).<br/>
 * 
 * <pre>
 * Usage: appng reload-site
 *   Options:
 *   * -n
 *        The site name.
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Parameters(commandDescription = "Reloads a site (site property supportReloadFile must be true).")
public class ReloadSite implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The site name.")
	private String siteName;

	public ReloadSite() {

	}

	ReloadSite(String siteName) {
		this.siteName = siteName;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		CheckSiteRunning checkSiteRunning = new CheckSiteRunning(siteName);
		checkSiteRunning.execute(cle);
		if (!checkSiteRunning.isRunning()) {
			throw new BusinessException(
					"The site '" + siteName + "' is currently not running and can not be reloaded.");
		}
		Site site = cle.getCoreService().getSiteByName(siteName);
		if (Boolean.TRUE.equals(site.getProperties().getBoolean(SiteProperties.SUPPORT_RELOAD_FILE))) {
			String rootDir = site.getProperties().getString(SiteProperties.SITE_ROOT_DIR);
			File reloadFile = new File(rootDir, ".reload");
			try {
				FileUtils.touch(reloadFile);
				CliEnvironment.out.println("Created reload marker " + reloadFile.getAbsolutePath());
			} catch (IOException e) {
				throw new BusinessException(e);
			}
		} else {
			String notSupported = String.format("Site %s does not support this, set %s=true to enable this feature.",
					siteName, SiteProperties.SUPPORT_RELOAD_FILE);
			CliEnvironment.out.println(notSupported);
		}
	}

}
