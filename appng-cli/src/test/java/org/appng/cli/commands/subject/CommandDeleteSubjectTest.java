/*
 * Copyright 2011-2021 the original author or authors.
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

import org.appng.api.model.Subject;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.domain.SubjectImpl;
import org.junit.Assert;

public class CommandDeleteSubjectTest extends AbstractCommandTest {

	public ExecutableCliCommand getCommand() {
		String name = "admin";
		SubjectImpl subject = new SubjectImpl();
		subject.setName(name);
		subject.setRealname("Admin Istrator");
		subject.setEmail("ad@min.com");
		subject.setLanguage("de");
		subject.setDescription("an admin");
		cliEnv.getCoreService().createSubject(subject);

		return new DeleteSubject(name);
	}

	public void validate() {
		Subject subject = cliEnv.getCoreService().getSubjectByEmail("ad@min.com");
		Assert.assertNull(subject);
	}

}
