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
package org.appng.cli.commands.application;

import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.cli.CliEnvironment;
import org.appng.cli.NoSuchSiteException;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.CacheProvider;

public class BaseApplication {

	void execute(CliEnvironment cle, String siteName, String applicationName, Mode mode) throws BusinessException {

		SiteImpl site = cle.getCoreService().getSiteByName(siteName);
		if (null == site) {
			throw new NoSuchSiteException(siteName);
		}

		Application application = cle.getCoreService().findApplicationByName(applicationName);
		if (null == application) {
			throw new BusinessException("Application not found: " + applicationName);
		}

		boolean isAssigned = site.getApplications().contains(application);
		if (Mode.LINK.equals(mode) && !isAssigned) {
			cle.getCoreService().assignApplicationToSite(site, application, true);
			// force to set the owner/group for the cache folder
			new CacheProvider(cle.getPlatformConfig()).getPlatformCache(site, application);
		} else if (Mode.UNLINK.equals(mode) && isAssigned) {
			cle.getCoreService().unlinkApplicationFromSite(site.getId(), application.getId());
		} else {
			throw new BusinessException("Application " + application.getName() + " with id " + application.getId()
					+ " is " + mode.word + " linked to Site " + site.getName() + " with id " + site.getId());
		}

	}

	public enum Mode {
		LINK("already"), UNLINK("not");

		private String word;

		private Mode(String word) {
			this.word = word;
		}

	}

}
