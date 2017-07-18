package org.appng.cli.commands.template;

import org.appng.api.BusinessException;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.cli.commands.repository.CommandCreateRepositoryTest;
import org.junit.Assert;

public class CommandInstallTemplateTest extends AbstractCommandTest {

	@Override
	public ExecutableCliCommand getCommand() {
		ExecutableCliCommand createRepository = new CommandCreateRepositoryTest().getCommand();
		try {
			createRepository.execute(cliEnv);
		} catch (BusinessException e) {
			Assert.fail(e.getMessage());
		}
		return parse(new InstallTemplate(), "-n", "appng-template", "-r", "localrepo", "-v", "0.8.0");
	}

	public void validate() throws BusinessException {
		// ok if command finished without an exception
	}

}
