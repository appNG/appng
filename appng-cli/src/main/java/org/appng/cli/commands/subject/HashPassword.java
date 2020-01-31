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
package org.appng.cli.commands.subject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.model.AuthSubject;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.security.DefaultPasswordPolicy;
import org.appng.core.security.PasswordHandler;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Hashes a password using the platform's default {@link PasswordHandler}.
 * 
 * <pre>
 * Usage: hash-pw [options] <cleartext-password>
 *   Options:
 *   -i
 *        Enables interactive mode.
 *        Default: false
 * </pre>
 * 
 * @author Matthias Müller
 */
@Parameters(commandDescription = "Hashes a password.")
public class HashPassword implements ExecutableCliCommand {

	@Parameter(required = false, description = "<cleartext-password>")
	private String password;

	@Parameter(names = "-i", required = false, description = "Enables interactive mode.")
	private boolean interactive = false;

	public HashPassword() {

	}

	HashPassword(String password) {
		this.password = password;
	}

	HashPassword(boolean interactive) {
		this.interactive = interactive;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		boolean passwordSet = StringUtils.isNotBlank(password);
		if (!(interactive || passwordSet)) {
			throw new BusinessException("Either password must be given or -i must be set!");
		}
		if (interactive) {
			runInteractive(cle);
			return;
		}
		String digest = savePasswordForSubject(cle, new SubjectImpl(), password);
		cle.setResult(digest);
	}

	private void runInteractive(CliEnvironment cle) throws BusinessException {
		try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in));) {
			try {
				CliEnvironment.out.println("Hashes a password. Type CTRL + C to quit.");
				while (true) {
					try {
						CliEnvironment.out.print("password: ");
						String commandLine = console.readLine();
						if (null != StringUtils.trimToNull(commandLine)) {
							String digest = savePasswordForSubject(cle, new SubjectImpl(), commandLine);
							CliEnvironment.out.print("hash: ");
							CliEnvironment.out.println(digest);
						}
					} catch (BusinessException e) {
						CliEnvironment.out.println(e.getMessage());
					}
					continue;
				}
			} catch (IOException e) {
				throw new BusinessException(e);
			}
		} catch (IOException e1) {
			throw new BusinessException(e1);
		}
	}

	static String savePasswordForSubject(CliEnvironment cle, AuthSubject subject, String password)
			throws BusinessException {
		String regEx = cle.getPlatformConfig().getString(Platform.Property.PASSWORD_POLICY_REGEX);
		String errorMessageKey = cle.getPlatformConfig().getString(Platform.Property.PASSWORD_POLICY_ERROR_MSSG_KEY);

		if (!new DefaultPasswordPolicy(regEx, errorMessageKey).isValidPassword(password.toCharArray())) {
			String errorMessage = cle.getMessageSource().getMessage(errorMessageKey, null, Locale.ENGLISH);
			throw new BusinessException(errorMessage);
		}
		PasswordHandler passwordHandler = cle.getCoreService().getDefaultPasswordHandler(subject);
		passwordHandler.applyPassword(password);
		return subject.getDigest();
	}
}
