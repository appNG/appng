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
package org.appng.cli.commands.repository;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.appng.api.BusinessException;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.NoSuchRepositoryException;
import org.appng.core.model.Repository;
import org.appng.core.model.RepositoryType;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Deletes a repository.<br/>
 * 
 * <pre>
 * Usage: appng delete-repository [options]
 *   Options:
 *   * -n
 *        The repository name.
 *     -r
 *        Remove the directory and its files (only LOCAL repositories).
 *        Default: false
 * </pre>
 * 
 * @author Matthias Herlitzius
 */
@Parameters(commandDescription = "Deletes a repository.")
public class DeleteRepository implements ExecutableCliCommand {

	@Parameter(names = "-n", required = true, description = "The repository name.")
	private String repositoryName;

	@Parameter(names = "-r", required = false, description = "Remove the directory and its files (only LOCAL repositories).")
	private boolean deleteDirectory;

	public DeleteRepository() {

	}

	DeleteRepository(String repositoryName, boolean deleteDirectory) {
		this.repositoryName = repositoryName;
		this.deleteDirectory = deleteDirectory;
	}

	public void execute(CliEnvironment cle) throws BusinessException {

		Repository repository = cle.getCoreService().getApplicationRepositoryByName(repositoryName);
		if (null != repository) {
			if (deleteDirectory && RepositoryType.LOCAL.equals(repository.getRepositoryType())) {
				try {
					FileUtils.forceDelete(new File(repository.getUri()));
				} catch (IOException e) {
					throw new BusinessException("Unable to delete repository: " + repository.getUri(), e);
				}
			}
			cle.getCoreService().deleteApplicationRepository(repository);
		} else {
			if (null == repository) {
				throw new NoSuchRepositoryException(repositoryName);
			}
		}

	}
}
