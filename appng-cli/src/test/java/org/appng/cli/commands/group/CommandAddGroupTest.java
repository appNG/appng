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
package org.appng.cli.commands.group;

import java.util.Arrays;

import org.appng.api.BusinessException;
import org.appng.api.model.Group;
import org.appng.cli.ExecutableCliCommand;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.domain.SubjectImpl;
import org.junit.Assert;

public class CommandAddGroupTest extends AbstractCommandTest {

	private SubjectImpl subject;

	@Override
	public ExecutableCliCommand getCommand() {
		subject = new SubjectImpl();
		subject.setName("admin");
		subject.setRealname("Admin Istrator");
		subject.setEmail("ad@min.com");
		subject.setLanguage("de");
		subject.setDescription("an admin");
		cliEnv.getCoreService().createSubject(subject);
		try {
			new CreateGroup("admins", "description").execute(cliEnv);
		} catch (BusinessException e) {
			throw new IllegalStateException("should not happen", e);
		}
		return new AddGroup(subject.getName(), Arrays.asList("admins"));
	}

	@Override
	public void validate() {
		Group group = cliEnv.getCoreService().getGroupByName("admins");
		Assert.assertEquals("admins", group.getName());
		Assert.assertTrue(group.getSubjects().contains(subject));
	}

}
