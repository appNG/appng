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

import org.appng.api.BusinessException;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.cli.commands.subject.HashPassword.PasswordSubject;
import org.appng.core.security.BCryptPasswordHandler;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HashPasswordTest extends AbstractCommandTest {

	private static final String SUPERSECRET = "supersecret";

	public HashPassword getCommand() {
		return new HashPassword(SUPERSECRET);
	}

	public void validate() {
		PasswordSubject authSubject = new PasswordSubject();
		authSubject.setDigest(cliEnv.getResult());
		BCryptPasswordHandler passwordHandler = new BCryptPasswordHandler(authSubject);
		passwordHandler.isValidPassword(SUPERSECRET);
	}
	
	@Ignore("only for local usage")
	public void testInteractive() throws BusinessException{
		new HashPassword(true).execute(cliEnv);
	}

}
