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
package org.appng.cli.commands.subject;

import java.util.Locale;

import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.model.Properties;
import org.appng.api.model.UserType;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.security.DefaultPasswordPolicy;
import org.appng.core.security.PasswordHandler;
import org.springframework.context.MessageSource;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Creates a subject.<br/>
 * 
 * <pre>
 * Usage: appng create-subject [options]
 *   Options:
 *     -d
 *        Description of the user.
 *        Default: <empty string>
 *   * -e
 *        The e-mail address.
 *     -l
 *        GUI language of the user.
 *        Default: de
 *   * -n
 *        The real name.
 *   * -p
 *        The password.
 *   * -u
 *        The user name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
@Parameters(commandDescription = "Creates a subject.")
public class CreateSubject implements ExecutableCliCommand {

	@Parameter(names = "-u", required = true, description = "The user name.")
	private String loginName;

	@Parameter(names = "-n", required = true, description = "The real name.")
	private String realName;

	@Parameter(names = "-e", required = true, description = "The e-mail address.")
	private String email;

	@Parameter(names = "-p", required = true, description = "The password.")
	private String password;

	@Parameter(names = "-l", required = false, description = "GUI language of the user.")
	private String language = "de";

	@Parameter(names = "-d", required = false, description = "Description of the user.")
	private String description = "";

	public CreateSubject() {

	}

	public CreateSubject(String loginName, String realName, String email, String password, String language,
			String description) {
		this.loginName = loginName;
		this.realName = realName;
		this.email = email;
		this.password = password;
		this.language = language;
		this.description = description;
	}

	public void execute(CliEnvironment cle) throws BusinessException {

		Properties platformConfig = cle.getPlatformConfig();
		String regEx = platformConfig.getString(Platform.Property.PASSWORD_POLICY_REGEX);
		String errorMessageKey = platformConfig.getString(Platform.Property.PASSWORD_POLICY_ERROR_MSSG_KEY);
		PasswordPolicy passwordPolicy = new DefaultPasswordPolicy(regEx, errorMessageKey);

		if (!passwordPolicy.isValidPassword(password.toCharArray())) {
			MessageSource messageSource = cle.getMessageSource();
			String errorMessage = messageSource.getMessage(errorMessageKey, null, Locale.ENGLISH);
			throw new BusinessException(errorMessage);
		}

		SubjectImpl subject = new SubjectImpl();
		subject.setName(loginName);
		subject.setRealname(realName);
		subject.setEmail(email);
		subject.setLanguage(language);
		subject.setDescription(description);
		subject.setUserType(UserType.LOCAL_USER);

		PasswordHandler passwordHandler = cle.getCoreService().getDefaultPasswordHandler(subject);
		passwordHandler.savePassword(password);

		cle.getCoreService().createSubject(subject);

	}
}
