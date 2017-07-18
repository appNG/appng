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
