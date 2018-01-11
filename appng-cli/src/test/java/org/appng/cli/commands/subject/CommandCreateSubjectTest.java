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

import static org.junit.Assert.assertTrue;

import org.appng.api.BusinessException;
import org.appng.api.model.Subject;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.security.BCryptPasswordHandler;
import org.junit.Assert;
import org.junit.Test;

public class CommandCreateSubjectTest extends AbstractCommandTest {

	@Override
	public CreateSubject getCommand() {
		return new CreateSubject("admin", "Admin Istrator", "ad@min.com", "test12", "de", "an admin");
	}

	@Test(expected = BusinessException.class)
	public void testException() throws BusinessException {
		new CreateSubject("admin", "Admin Istrator", "ad@min.com", "test", "de", "an admin").execute(cliEnv);
	}

	@Override
	public void validate() {
		Subject subject = cliEnv.getCoreService().getSubjectByEmail("ad@min.com");
		Assert.assertEquals("admin", subject.getAuthName());
		Assert.assertEquals("Admin Istrator", subject.getRealname());
		Assert.assertEquals("ad@min.com", subject.getEmail());
		Assert.assertEquals("an admin", subject.getDescription());
		Assert.assertEquals("de", subject.getLanguage());
		Assert.assertNotNull(subject.getDigest());
		Assert.assertNull(subject.getSalt());
		assertTrue(subject.getDigest().startsWith(BCryptPasswordHandler.getPrefix()));

	}

}
