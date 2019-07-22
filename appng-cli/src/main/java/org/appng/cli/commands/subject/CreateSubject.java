/*
 * Copyright 2011-2019 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.model.UserType;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.SubjectImpl;

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
 *     -h
 *        Has the password already been hashed using 'hash-pw &lt;password&gt'?
 *        Default: false
 *     -l
 *        GUI language of the user.
 *        Default: de
 *   * -n
 *        The real name.
 *   * -p
 *        The password (The password, mandatory for type LOCAL_USER.).
 *   * -t
 *        The type of the user.
 *        Default: LOCAL_USER
 *        Possible Values: [LOCAL_USER, GLOBAL_USER, GLOBAL_GROUP]
 *   * -u
 *        The user name.
 * </pre>
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
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

	@Parameter(names = "-p", required = false, description = "The password, mandatory for type LOCAL_USER.")
	private String password;

	@Parameter(names = "-h", required = false, description = "Has the password already been hashed using 'hash-pw <password>'?")
	private boolean passwordHashed = false;

	@Parameter(names = "-l", required = false, description = "GUI language of the user.")
	private String language = "de";

	@Parameter(names = "-d", required = false, description = "Description of the user.")
	private String description = "";

	@Parameter(names = "-t", required = false, description = "The type of the user.")
	private UserType type = UserType.LOCAL_USER;

	public CreateSubject() {

	}

	CreateSubject(String loginName, String realName, String email, String password, String language, String description,
			UserType type, boolean passwordHashed) {
		this.loginName = loginName;
		this.realName = realName;
		this.email = email;
		this.password = password;
		this.language = language;
		this.description = description;
		this.type = type;
		this.passwordHashed = passwordHashed;
	}

	public void execute(CliEnvironment cle) throws BusinessException {
		SubjectImpl subject = new SubjectImpl();
		if (UserType.LOCAL_USER.equals(type)) {
			if (StringUtils.isBlank(password)) {
				throw new BusinessException(String.format("-p is mandatory for type %s", type.name()));
			}
			if (!passwordHashed) {
				HashPassword.savePasswordForSubject(cle, subject, password);
			}
		} else if (StringUtils.isNotBlank(password)) {
			throw new BusinessException(String.format("-p is not allowed for type %s", type.name()));
		}
		if (null != cle.getCoreService().getSubjectByName(loginName, false)) {
			throw new BusinessException(String.format("Subject with name '%s' already exists.", loginName));
		}

		subject.setName(loginName);
		subject.setRealname(realName);
		subject.setEmail(email);
		subject.setLanguage(language);
		subject.setDescription(description);
		subject.setUserType(type);
		if (passwordHashed) {
			subject.setDigest(password);
		}

		cle.getCoreService().createSubject(subject);

	}
}
