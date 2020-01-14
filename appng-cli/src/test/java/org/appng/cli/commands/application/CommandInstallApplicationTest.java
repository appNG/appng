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
package org.appng.cli.commands.application;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.cli.commands.repository.CommandCreateRepositoryTest;
import org.junit.Assert;

public class CommandInstallApplicationTest extends AbstractCommandTest {

	@Override
	public ExecutableCliCommand getCommand() {
		ExecutableCliCommand createRepository = new CommandCreateRepositoryTest().getCommand();
		try {
			createRepository.execute(cliEnv);
		} catch (BusinessException e) {
			Assert.fail(e.getMessage());
		}
		return parse(new InstallApplication(), "-n", "demo-application", "-r", "localrepo", "-v", "1.5.4");
	}

	public void validate() throws BusinessException {
		parse(new ListApplications(), "-t").execute(cliEnv);
		String result = cliEnv.getResult();
		Assert.assertEquals("1\tdemo-application\tDemo Application\tfalse\t", result.split(StringUtils.LF)[2]);
	}

}
