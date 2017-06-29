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
package org.appng.cli.commands.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Creates a repository.<br/>
 * 
 * <pre>
 * Usage: appng create-repository [options]
 *   Options:
 *     -d
 *        The repository description.
 *     -e
 *        Enable repository.
 *        Default: true
 *     -m
 *        The repository mode (ALL, STABLE, SNAPSHOT).
 *        Default: ALL
 *   * -n
 *        The repository name.
 *     -p
 *        Publish repository.
 *        Default: false
 *     -r
 *        The name of the remote repository (required for repository type REMOTE).
 *     -s
 *        Use strict mode.
 *        Default: false
 *     -t
 *        The repository type (LOCAL, REMOTE).
 *        Default: LOCAL
 *   * -u
 *        The path (URI) to the repository.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Creates a repository.")
public class CreateRepository implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The repository name.")
	private String name;

	@Parameter(names = "-d", required = false, description = "The repository description.")
	private String description;

	@Parameter(names = "-t", required = false, description = "The repository type (LOCAL, REMOTE).")
	private RepositoryType type = RepositoryType.getDefault();

	@Parameter(names = "-m", required = false, description = "The repository mode (ALL, STABLE, SNAPSHOT).")
	private RepositoryMode mode = RepositoryMode.getDefault();

	@Parameter(names = "-u", required = true, description = "The path (URI) to the repository.")
	private String uriString;

	@Parameter(names = "-r", required = false, description = "The name of the remote repository (required for repository type REMOTE).")
	private String remoteRepositoryName;

	@Parameter(names = "-e", description = "Enable repository.", arity = 1)
	private boolean isActive = true;

	@Parameter(names = "-p", description = "Publish repository.")
	private boolean isPublished = false;

	@Parameter(names = "-s", description = "Use strict mode.")
	private Boolean isStrict = false;

	public CreateRepository() {

	}

	public void execute(CliEnvironment cle) throws BusinessException {
		try {
			if (type.equals(RepositoryType.REMOTE) && StringUtils.isBlank(remoteRepositoryName)) {
				throw new BusinessException("The missing parameter -r must contain the name of the remote repository.");
			}
			URI uri = new URI(uriString);
			if (type.equals(RepositoryType.LOCAL)) {
				File path = new File(uri);
				if (path.exists()) {
					if (!path.isDirectory()) {
						throw new BusinessException("Must be a directory, but is a file: " + path.getAbsolutePath());
					}
				} else {
					try {
						FileUtils.forceMkdir(path);
					} catch (IOException e) {
						throw new BusinessException("Can not create directory: " + path.getAbsolutePath(), e);
					}
				}
			}

			RepositoryImpl repository = new RepositoryImpl();
			repository.setName(name);
			repository.setDescription(description);
			repository.setRepositoryType(type);
			repository.setRepositoryMode(mode);
			repository.setRemoteRepositoryName(remoteRepositoryName);
			repository.setActive(isActive);
			repository.setPublished(isPublished);
			repository.setStrict(isStrict);
			repository.setUri(uri);

			RepositoryCacheFactory.validateRepositoryURI(repository);
			cle.getCoreService().createRepository(repository);
		} catch (URISyntaxException u) {
			throw new BusinessException("Not a valid URI: " + uriString, u);
		} catch (Exception e) {
			throw new BusinessException(e);
		}
	}
}
