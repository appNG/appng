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

import java.io.File;

import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.NoSuchRepositoryException;
import org.appng.cli.commands.FileOwner;
import org.appng.core.model.Repository;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.service.CoreService;
import org.appng.core.service.InitializerService;
import org.appng.xml.application.PackageInfo;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Installs an application.<br/>
 * 
 * <pre>
 * Usage: appng install-application [options]
 *   Options:
 *     -p
 *        Install as privileged application.
 *        Default: false
 *     -f
 *        Application will be filebased.
 *        Default: false
 *     -h
 *        Application will be hidden.
 *        Default: false
 *   * -n
 *        The name of the application.
 *   * -r
 *        The name of the repository.
 *     -t
 *        The timestamp of the application. Required if a specific snapshot version
 *        should be provisioned.
 *   * -v
 *        The version of the application.
 * </pre>
 * 
 * @author Matthias Herlitzius
 */
@Parameters(commandDescription = "Imports a application.")
public class InstallApplication implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The name of the application.")
	private String applicationName;

	@Parameter(names = "-v", required = true, description = "The version of the application.")
	private String applicationVersion;

	@Parameter(names = "-t", required = false, description = "The timestamp of the application. Required if a specific snapshot version should be provisioned.")
	private String applicationTimestamp;

	@Parameter(names = "-r", required = true, description = "The name of the repository.")
	private String repositoryName;

	@Parameter(names = { "-p", "-c" }, required = false, description = "Install as privileged application.")
	private boolean isPrivileged = false;

	@Parameter(names = "-h", required = false, description = "Application will be hidden.")
	private boolean isHidden = false;

	@Parameter(names = "-f", required = false, description = "Application will be filebased.")
	private boolean isFileBased = false;

	public void execute(CliEnvironment cle) throws BusinessException {
		Repository repository = cle.getCoreService().getApplicationRepositoryByName(repositoryName);
		if (null == repository) {
			throw new NoSuchRepositoryException(repositoryName);
		}
		if (null == RepositoryCacheFactory.instance()) {
			RepositoryCacheFactory.init(cle.getPlatformConfig());
		}
		CoreService coreService = cle.getCoreService();
		PackageInfo installedPackage = coreService.installPackage(repository.getId(), applicationName,
				applicationVersion, applicationTimestamp, isPrivileged, isHidden, isFileBased);
		Application application = coreService.findApplicationByName(installedPackage.getName());
		if (null != application) {
			if (isFileBased && !application.isFileBased()) {
				cle.setResult("application was already present and located in the database, so -f was ignored.");
			}
			if (application.isFileBased()) {
				File applicationFolder = coreService.getApplicationFolder(null, application.getName());
				String user = cle.getCliConfig().getProperty(InitializerService.APPNG_USER);
				String group = cle.getCliConfig().getProperty(InitializerService.APPNG_GROUP);
				if (!new FileOwner(applicationFolder).own(user, group)) {
					cle.setResult("failed to set directory permissions for " + applicationFolder.getAbsolutePath());
				}
			}
		}
	}
}
