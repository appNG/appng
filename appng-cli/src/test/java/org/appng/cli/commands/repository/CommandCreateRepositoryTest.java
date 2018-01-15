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
package org.appng.cli.commands.repository;

import java.io.File;
import java.net.URI;

import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.model.Repository;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.junit.Assert;

public class CommandCreateRepositoryTest extends AbstractCommandTest {

	static final URI REPO_URI = new File("target/test-classes/localrepo").getAbsoluteFile().toURI();

	public ExecutableCliCommand getCommand() {
		String[] args = new String[] { "-n", "localrepo", "-d", "a local repository", "-t", RepositoryType.LOCAL.name(),
				"-m", RepositoryMode.ALL.name(), "-u", REPO_URI.toString() };
		return parse(new CreateRepository(), args);
	}

	public void validate() {
		Repository repo = cliEnv.getCoreService().getApplicationRepositoryByName("localrepo");

		Assert.assertEquals("localrepo", repo.getName());
		Assert.assertEquals("a local repository", repo.getDescription());
		Assert.assertEquals(RepositoryMode.ALL, repo.getRepositoryMode());
		Assert.assertEquals(RepositoryType.LOCAL, repo.getRepositoryType());
		Assert.assertEquals(REPO_URI, repo.getUri());
		Assert.assertTrue(repo.isActive());
		Assert.assertFalse(repo.isPublished());
		Assert.assertFalse(repo.isStrict());
	}

}
